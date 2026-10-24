# 关键业务代码示例

以下为关键实现节选，完整代码见对应源码路径。

## 1. JWT 认证与接口权限拦截

`backend/src/main/java/com/enterprise/archive/security/AuthInterceptor.java`

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        return true;
    }
    String token = resolveToken(request);
    if (token == null) {
        throw new BizException(401, "请先登录后再操作");
    }
    try {
        Long userId = jwtUtil.parseUserId(token);
        UserContext.set(authService.loadLoginUser(userId));
    } catch (JwtException | IllegalArgumentException e) {
        throw new BizException(401, "登录状态已失效，请重新登录");
    }

    if (handler instanceof HandlerMethod handlerMethod) {
        RequirePerm requirePerm = handlerMethod.getMethodAnnotation(RequirePerm.class);
        if (requirePerm == null) {
            requirePerm = handlerMethod.getBeanType().getAnnotation(RequirePerm.class);
        }
        if (requirePerm != null && !UserContext.hasPerm(requirePerm.value())) {
            throw new BizException(403, "您没有执行此操作的权限");
        }
    }
    return true;
}

@Override
public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                            Object handler, Exception ex) {
    UserContext.clear();
}
```

使用方式：

```java
@PostMapping("/{id}/submit")
@RequirePerm("contract:submit")
@OpLog(module = "合同档案", action = "提交归档")
public Result<Void> submit(@PathVariable Long id) {
    contractService.submit(id);
    return Result.ok();
}
```

## 2. 操作日志切面（不可删除审计）

`backend/src/main/java/com/enterprise/archive/aspect/OperationLogAspect.java`

```java
@Around("@annotation(opLog)")
public Object around(ProceedingJoinPoint joinPoint, OpLog opLog) throws Throwable {
    long start = System.currentTimeMillis();
    String status = "SUCCESS";
    String error = null;
    try {
        return joinPoint.proceed();
    } catch (Throwable throwable) {
        status = "FAIL";
        error = throwable.getMessage();
        throw throwable;
    } finally {
        LoginUser user = UserContext.getOrNull();
        HttpServletRequest request = currentRequest();
        String params = serializeArgs(joinPoint);
        logService.record(user == null ? null : user.getId(),
                user == null ? null : user.getRealName(),
                opLog.module(), opLog.action(), status, error,
                System.currentTimeMillis() - start, request, params);
    }
}
```

参数序列化会跳过文件/请求对象，并把 `password` 替换为 `***`。日志表没有删除接口，数据库触发器也拒绝物理删除。

## 3. 归档审核与唯一档案号

`backend/src/main/java/com/enterprise/archive/service/ContractService.java`

```java
@Transactional
public void review(Long id, ReviewRequest request) {
    ContractInfo contract = requireContract(id);
    if (!ContractStatus.PENDING.equals(contract.getStatus())) {
        throw new BizException("仅审核中的合同可以执行审核");
    }
    String action = request.getAction();
    if (!BizConstants.ReviewAction.APPROVE.equals(action)
            && !BizConstants.ReviewAction.REJECT.equals(action)) {
        throw new BizException("审核动作不合法");
    }

    ArchiveReviewRecord review = new ArchiveReviewRecord();
    review.setContractId(contract.getId());
    review.setAction(action);
    review.setOpinion(request.getOpinion());
    review.setReviewerId(UserContext.userId());
    review.setReviewTime(LocalDateTime.now());

    if (BizConstants.ReviewAction.APPROVE.equals(action)) {
        validateForArchive(contract);
        String archiveNo = generateArchiveNo();
        contract.setStatus(ContractStatus.ARCHIVED);
        contract.setArchiveNo(archiveNo);
        contract.setArchiveTime(LocalDateTime.now());
        contract.setArchiveBy(UserContext.userId());
        review.setArchiveNo(archiveNo);
        contractMapper.updateById(contract);
    } else {
        contract.setStatus(ContractStatus.REJECTED);
        contractMapper.updateById(contract);
    }
    reviewMapper.insert(review);
}

private synchronized String generateArchiveNo() {
    String prefix = "DA" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    ContractInfo last = contractMapper.selectOne(new LambdaQueryWrapper<ContractInfo>()
            .likeRight(ContractInfo::getArchiveNo, prefix)
            .orderByDesc(ContractInfo::getArchiveNo)
            .last("LIMIT 1"));
    int seq = 1;
    if (last != null && last.getArchiveNo() != null
            && last.getArchiveNo().length() > prefix.length()) {
        try {
            seq = Integer.parseInt(last.getArchiveNo().substring(prefix.length())) + 1;
        } catch (NumberFormatException ignored) {
        }
    }
    return prefix + String.format("%04d", seq);
}
```

数据库对 `archive_no` 建唯一索引，作为并发生成的最终兜底。

## 4. 归档合同只读与变更快照

```java
@Transactional
public void applyChange(ChangeApplyRequest request) {
    ContractInfo contract = requireContract(request.getContractId());
    assertVisible(contract);
    if (!ContractStatus.ARCHIVED_STATUS.contains(contract.getStatus())) {
        throw new BizException("仅已归档合同可以申请变更");
    }
    Long pending = changeMapper.selectCount(new LambdaQueryWrapper<ArchiveChangeRecord>()
            .eq(ArchiveChangeRecord::getContractId, contract.getId())
            .eq(ArchiveChangeRecord::getStatus, ChangeStatus.PENDING));
    if (pending > 0) {
        throw new BizException("该合同已有待审批的变更申请");
    }

    Map<String, Object> beforeMap = contractSnapshot(contract);
    ContractInfo target = cloneContract(contract);
    applyChangeFields(target, request.getChangeFields());
    validateDates(target.getEffectiveDate(), target.getExpireDate());
    Map<String, Object> afterMap = contractSnapshot(target);

    ArchiveChangeRecord record = new ArchiveChangeRecord();
    record.setContractId(contract.getId());
    record.setChangeType(request.getChangeType());
    record.setChangeReason(request.getChangeReason());
    record.setBeforeContent(objectMapper.writeValueAsString(beforeMap));
    record.setAfterContent(objectMapper.writeValueAsString(afterMap));
    record.setStatus(ChangeStatus.PENDING);
    record.setApplicantId(UserContext.userId());
    record.setApplyTime(LocalDateTime.now());
    changeMapper.insert(record);
}
```

普通编辑接口会对已归档合同直接拒绝：

```java
if (!Set.of(ContractStatus.DRAFT, ContractStatus.REJECTED).contains(contract.getStatus())) {
    throw new BizException("仅草稿或已驳回合同允许编辑，归档合同请走变更审批流程");
}
```

变更审批通过后才把 `after_content` 写回主档，审核/变更历史永久保留。

## 5. 附件安全存储与下载鉴权

`backend/src/main/java/com/enterprise/archive/service/AttachmentService.java`

```java
private AttachmentVO store(ContractInfo contract, MultipartFile file, String originalName,
                           String ext, String fileType) throws IOException {
    Path baseDir = storageBase();
    String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    Path targetDir = baseDir.resolve(dateDir).normalize();
    if (!targetDir.startsWith(baseDir)) {
        throw new BizException("文件存储路径非法");
    }
    Files.createDirectories(targetDir);

    String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
    Path target = targetDir.resolve(storedName).normalize();
    if (!target.startsWith(baseDir)) {
        throw new BizException("文件存储路径非法");
    }
    try (InputStream in = file.getInputStream()) {
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }

    ContractAttachment attachment = new ContractAttachment();
    attachment.setContractId(contract.getId());
    attachment.setFileName(storedName);
    attachment.setOriginalName(originalName);
    attachment.setFilePath(target.toString());
    attachment.setFileExt(ext);
    attachment.setFileSize(file.getSize());
    attachment.setChecksum(sha256(target));
    attachment.setContentText(extractText(target, ext));
    attachment.setUploaderId(UserContext.userId());
    attachment.setUploadTime(LocalDateTime.now());
    attachmentMapper.insert(attachment);
    return toVO(attachment, UserContext.get().getRealName());
}
```

归档附件下载校验：

```java
private void assertCanRead(ContractInfo contract, boolean download) {
    LoginUser user = UserContext.get();
    if (download) {
        if (!UserContext.hasPerm("contract:download")) {
            throw new BizException(403, "您没有附件下载权限");
        }
        if (isArchived(contract)) {
            if (!user.has("archive:download:any")
                    && !hasActiveBorrow(contract.getId(), user.getId())) {
                throw new BizException(403, "该档案未开放下载，请先提交借阅申请并审批通过");
            }
        } else if (!Objects.equals(contract.getCreateBy(), user.getId())
                && !Objects.equals(contract.getOwnerId(), user.getId())
                && !user.has("contract:view:all")) {
            throw new BizException(403, "您无权下载该合同附件");
        }
        return;
    }
    if (!UserContext.hasPerm("attachment:preview")) {
        throw new BizException(403, "您没有附件预览权限");
    }
}
```

有效借阅判断：

```java
public boolean hasActiveBorrow(Long contractId, Long userId) {
    LocalDateTime now = LocalDateTime.now();
    return borrowMapper.selectCount(new LambdaQueryWrapper<BorrowApply>()
            .eq(BorrowApply::getContractId, contractId)
            .eq(BorrowApply::getApplicantId, userId)
            .eq(BorrowApply::getStatus, "APPROVED")
            .le(BorrowApply::getStartTime, now)
            .ge(BorrowApply::getEndTime, now)) > 0;
}
```

## 6. 预警扫描与幂等去重

`backend/src/main/java/com/enterprise/archive/service/WarningService.java`

```java
private int createWarnings(Long contractId, String type, LocalDate warningDate,
                           String message, String bizKey, Set<Long> receiverIds) {
    int created = 0;
    for (Long receiverId : receiverIds) {
        if (receiverId == null) continue;
        Long exists = warningMapper.selectCount(new LambdaQueryWrapper<SysWarning>()
                .eq(SysWarning::getBizKey, bizKey)
                .eq(SysWarning::getReceiverId, receiverId));
        if (exists > 0) continue;

        SysWarning warning = new SysWarning();
        warning.setContractId(contractId);
        warning.setWarningType(type);
        warning.setWarningDate(warningDate);
        warning.setMessage(message);
        warning.setBizKey(bizKey);
        warning.setReceiverId(receiverId);
        warning.setReadStatus(0);
        warningMapper.insert(warning);
        created++;
    }
    return created;
}
```

`biz_key` 由业务类型、合同/节点 ID、业务日期组成，如 `EXPIRY-12-2027-10-24`；表上 `(biz_key, receiver_id)` 唯一索引防止重复扫描重复推送。

定时任务：

```java
@Scheduled(cron = "${app.warning.cron:0 10 1 * * ?}")
public void scanWarnings() {
    warningService.scan();
}
```

## 7. Excel 模板导入（含说明行跳过）

`ExcelService` 读取第一个 Sheet，第 1 行为表头，第 2 行为合并的填写说明，数据从第 3 行开始。合并单元格文字会落在“合同编号”列，因此必须显式跳过说明行：

```java
for (int i = 1; i <= sheet.getLastRowNum(); i++) {
    Row row = sheet.getRow(i);
    if (row == null || isBlankRow(row, formatter)) {
        continue;
    }
    String contractNo = getString(row, headerIndex.get("合同编号"), formatter);
    // 模板第 2 行为合并单元格的填写说明，合并区域的文字会落在合同编号列，需直接跳过。
    if (contractNo.startsWith("说明")) {
        continue;
    }
    // ...校验部门、字典、日期并保存为草稿
}
```

导入结果返回成功数、失败数、逐行错误信息与已生成合同 ID。单次最多 1000 条。

## 8. 前端路由守卫与按钮权限

`frontend/src/router/index.js`

```js
router.beforeEach(async (to) => {
  const store = useUserStore(pinia)
  document.title = to.meta.title ? `${to.meta.title} - 档案合同管理` : '企业档案合同资料管理分析系统'

  if (to.meta.public) {
    if (store.token && to.path === '/login') {
      await store.ensureProfile()
      return getLandingPath(store)
    }
    return true
  }
  if (!store.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  try {
    await store.ensureProfile()
  } catch {
    store.clear()
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.perm && !store.hasPerm(to.meta.perm)) {
    ElMessage.warning('您没有访问该页面的权限')
    return getLandingPath(store)
  }
  return true
})
```

`frontend/src/main.js`

```js
app.directive('perm', {
  mounted(el, binding) {
    const store = useUserStore()
    const value = binding.value
    const allowed = Array.isArray(value)
      ? value.some((p) => store.hasPerm(p))
      : store.hasPerm(value)
    if (!allowed && el.parentNode) {
      el.parentNode.removeChild(el)
    }
  }
})
```

页面使用：

```vue
<el-button v-perm="'contract:add'" type="primary">录入合同</el-button>
<el-button v-if="userStore.hasPerm('contract:export')" @click="exportData">导出台账</el-button>
```

## 9. 数据库数据保护触发器

`sql/01_schema.sql`

```sql
CREATE TRIGGER trg_log_forbid_delete BEFORE DELETE ON sys_operation_log
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '操作日志禁止删除';
END$$

CREATE TRIGGER trg_contract_forbid_delete BEFORE DELETE ON contract_info
FOR EACH ROW
BEGIN
  IF OLD.archive_no IS NOT NULL OR OLD.status IN ('ARCHIVED', 'EXPIRED') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '已归档合同档案禁止物理删除';
  END IF;
END$$

CREATE TRIGGER trg_contract_forbid_archive_delete BEFORE UPDATE ON contract_info
FOR EACH ROW
BEGIN
  IF OLD.archive_no IS NOT NULL AND NEW.deleted = 1 AND OLD.deleted = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '已归档合同档案禁止删除，如需处置请走档案变更审批流程';
  END IF;
END$$
```

应用层的 `@TableLogic` 负责正常“逻辑删除”，触发器负责阻止绕过应用层的物理删除与非法逻辑删除。

## 10. 前后端一体化托管

前端构建产物位于 `backend/src/main/resources/static`，随 jar/war 一起发布。为避免前端页面路由与 REST 接口冲突，`WebConfig` 只给 `@RestController` 统一添加 `/api` 前缀，拦截器也只校验 `/api/**`：

```java
@Override
public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class));
}

@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/auth/login", "/error");
}
```

History 模式下刷新 `/contracts/12`、`/ledger` 等地址会命中后端控制器，因此由 `SpaController` 回退到 `index.html`：

```java
@Controller
public class SpaController {

    @GetMapping({
            "/", "/login", "/dashboard", "/contracts", "/contracts/new",
            "/contracts/{id}", "/contracts/{id}/edit",
            "/archive/review", "/archive/changes",
            "/borrow/my", "/borrow/approval",
            "/warnings", "/stats", "/ledger",
            "/system/users", "/system/roles", "/system/depts",
            "/system/dicts", "/system/config", "/system/logs"
    })
    public String index() {
        return "forward:/index.html";
    }
}
```

部署到外部 Tomcat 时，由 `ServletInitializer` 接管 Spring 容器启动：

```java
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ArchiveApplication.class);
    }
}
```

WAR 模式下前端 base 与接口前缀由构建变量决定：

```js
// vite.config.js
base: process.env.VITE_BASE || '/'

// router/index.js
history: createWebHistory(import.meta.env.BASE_URL)

// api/request.js
baseURL: import.meta.env.VITE_API_BASE || `${import.meta.env.BASE_URL}api`
```

Maven 在 `prepare-package` 阶段自动复制前端产物，保证 `mvn package` 得到的 jar/war 可直接运行：

```xml
<execution>
  <id>copy-frontend-dist</id>
  <phase>prepare-package</phase>
  <goals><goal>copy-resources</goal></goals>
  <configuration>
    <outputDirectory>${project.build.outputDirectory}/static</outputDirectory>
    <resources>
      <resource>
        <directory>${project.basedir}/../frontend/dist</directory>
        <filtering>false</filtering>
      </resource>
    </resources>
  </configuration>
</execution>
```

最终访问 `http://localhost:8080/` 打开页面，接口仍为 `http://localhost:8080/api/**`。

## 11. 普通用户自助注册

注册接口公开，但不允许客户端指定角色、部门或状态，服务端固定创建 `VIEWER` 账号并直接返回登录态。

`RegisterRequest` 只接收必要字段：

```java
@NotBlank(message = "登录账号不能为空")
@Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{3,29}$",
        message = "账号需以字母开头，长度为 4-30 位，可包含字母、数字和下划线")
private String username;

@NotBlank(message = "登录密码不能为空")
@Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
private String password;

@NotBlank(message = "真实姓名不能为空")
@Size(min = 2, max = 50, message = "姓名长度需为 2-50 个字符")
private String realName;

@Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
private String phone;

@Email(message = "邮箱格式不正确")
private String email;
```

`AuthService.register` 固定默认角色，并用唯一索引兜底并发重复注册：

```java
private static final String DEFAULT_ROLE_CODE = "VIEWER";

@Transactional
public LoginVO register(RegisterRequest request, HttpServletRequest servletRequest) {
    String username = request.getUsername().trim();

    SysUser exists = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUsername, username).last("LIMIT 1"));
    if (exists != null) {
        throw new BizException("登录账号已存在，请更换");
    }

    SysRole defaultRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
            .eq(SysRole::getRoleCode, DEFAULT_ROLE_CODE)
            .eq(SysRole::getStatus, 1)
            .last("LIMIT 1"));
    if (defaultRole == null) {
        throw new BizException("系统未配置默认角色，请联系管理员");
    }

    SysUser user = new SysUser();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setRealName(request.getRealName().trim());
    user.setPhone(trimToNull(request.getPhone()));
    user.setEmail(trimToNull(request.getEmail()));
    user.setStatus(1);
    user.setRemark("用户自助注册");
    try {
        userMapper.insert(user);
    } catch (DuplicateKeyException e) {
        throw new BizException("登录账号已存在，请更换");
    }

    SysUserRole relation = new SysUserRole();
    relation.setUserId(user.getId());
    relation.setRoleId(defaultRole.getId());
    userRoleMapper.insert(relation);

    LoginUser loginUser = buildLoginUser(user);
    LoginVO vo = new LoginVO();
    vo.setToken(jwtUtil.generate(user.getId(), user.getUsername()));
    vo.setExpiresIn(jwtUtil.getExpireSeconds());
    vo.setUser(toProfile(loginUser));
    logService.record(user.getId(), user.getUsername(), "登录认证", "用户注册",
            "SUCCESS", null, 0, servletRequest, "{\"username\":\"" + username + "\"}");
    return vo;
}
```

拦截器放行注册接口：

```java
registry.addInterceptor(authInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns("/api/auth/login", "/api/auth/register", "/error");
```

前端登录页提供“立即注册”弹窗，提交成功后调用 `userStore.register` 保存 token 与用户信息，并按权限跳转首个可用页面，无需再次登录。

注册成功会写入 `sys_operation_log`，该表按数据保护约束禁止删除，便于追溯账号来源。
