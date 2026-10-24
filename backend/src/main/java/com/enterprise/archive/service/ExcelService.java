package com.enterprise.archive.service;

import com.enterprise.archive.common.BizException;
import com.enterprise.archive.common.UserContext;
import com.enterprise.archive.dto.ContractQuery;
import com.enterprise.archive.dto.ContractSaveRequest;
import com.enterprise.archive.dto.StatsQuery;
import com.enterprise.archive.entity.SysDept;
import com.enterprise.archive.entity.SysDictItem;
import com.enterprise.archive.entity.SysUser;
import com.enterprise.archive.vo.ContractVO;
import com.enterprise.archive.vo.ImportResultVO;
import com.enterprise.archive.vo.StatItemVO;
import com.enterprise.archive.vo.StatsVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private final ContractService contractService;
    private final StatsService statsService;
    private final DictService dictService;
    private final DeptService deptService;
    private final UserService userService;

    private static final String[] CONTRACT_HEADERS = {
            "合同编号*", "合同名称*", "甲方*", "乙方*", "签订日期*", "生效日期", "到期日期",
            "合同金额(元)*", "合同类型*", "归属部门*", "负责人*", "密级*", "备注"
    };

    public void exportContracts(ContractQuery query, HttpServletResponse response, String fileName) {
        List<ContractVO> list = contractService.exportList(query);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("合同台账");
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
            String[] headers = {"序号", "合同编号", "合同名称", "甲方", "乙方", "签订日期", "生效日期",
                    "到期日期", "合同金额(元)", "合同类型", "归属部门", "负责人", "密级", "状态",
                    "归档编号", "归档时间", "附件数", "创建人", "创建时间"};
            createHeader(sheet, headers, headerStyle);
            int rowIndex = 1;
            for (ContractVO c : list) {
                Row row = sheet.createRow(rowIndex);
                int col = 0;
                row.createCell(col++).setCellValue(rowIndex);
                row.createCell(col++).setCellValue(nvl(c.getContractNo()));
                row.createCell(col++).setCellValue(nvl(c.getContractName()));
                row.createCell(col++).setCellValue(nvl(c.getPartyA()));
                row.createCell(col++).setCellValue(nvl(c.getPartyB()));
                row.createCell(col++).setCellValue(date(c.getSignDate()));
                row.createCell(col++).setCellValue(date(c.getEffectiveDate()));
                row.createCell(col++).setCellValue(date(c.getExpireDate()));
                Cell amountCell = row.createCell(col++);
                if (c.getAmount() != null) {
                    amountCell.setCellValue(c.getAmount().doubleValue());
                    amountCell.setCellStyle(moneyStyle);
                }
                row.createCell(col++).setCellValue(nvl(c.getContractTypeName()));
                row.createCell(col++).setCellValue(nvl(c.getDeptName()));
                row.createCell(col++).setCellValue(nvl(c.getOwnerName()));
                row.createCell(col++).setCellValue(nvl(c.getSecurityLevelName()));
                row.createCell(col++).setCellValue(nvl(c.getStatusName()));
                row.createCell(col++).setCellValue(nvl(c.getArchiveNo()));
                row.createCell(col++).setCellValue(c.getArchiveTime() == null ? "" : c.getArchiveTime().toString().replace('T', ' '));
                row.createCell(col++).setCellValue(c.getAttachmentCount() == null ? 0 : c.getAttachmentCount());
                row.createCell(col++).setCellValue(nvl(c.getCreateByName()));
                row.createCell(col).setCellValue(c.getCreateTime() == null ? "" : c.getCreateTime().toString().replace('T', ' '));
                rowIndex++;
            }
            finishSheet(sheet, headers.length, rowIndex);
            write(workbook, response, fileName);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("导出失败：" + e.getMessage());
        }
    }

    public void exportStats(StatsQuery statsQuery, HttpServletResponse response) {
        StatsVO stats = statsService.overview(statsQuery);
        ContractQuery contractQuery = new ContractQuery();
        contractQuery.setSignDateStart(statsQuery.getStartDate());
        contractQuery.setSignDateEnd(statsQuery.getEndDate());
        contractQuery.setDeptId(statsQuery.getDeptId());
        contractQuery.setContractType(statsQuery.getContractType());
        List<ContractVO> details = contractService.exportList(contractQuery);
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            Sheet sheet = workbook.createSheet("多维统计分析");
            int rowIndex = 0;
            Row title = sheet.createRow(rowIndex++);
            title.createCell(0).setCellValue("档案合同统计分析报表");
            title.getCell(0).setCellStyle(titleStyle(workbook));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
            rowIndex++;
            rowIndex = writeSummary(sheet, rowIndex, "统计概览", new String[]{"指标", "数值"}, headerStyle);
            writePair(sheet, rowIndex++, "合同总份数", String.valueOf(stats.getTotalCount()));
            writePair(sheet, rowIndex++, "合同总金额(元)", stats.getTotalAmount() == null ? "0" : stats.getTotalAmount().toPlainString());
            writePair(sheet, rowIndex++, "统计时间范围",
                    (statsQuery.getStartDate() == null ? "不限" : statsQuery.getStartDate())
                            + " 至 " + (statsQuery.getEndDate() == null ? "不限" : statsQuery.getEndDate()));
            rowIndex += 2;
            rowIndex = writeBlock(sheet, rowIndex, "按合同类型统计", stats.getTypeStats(), headerStyle);
            rowIndex = writeBlock(sheet, rowIndex, "按归属部门统计", stats.getDeptStats(), headerStyle);
            rowIndex = writeBlock(sheet, rowIndex, "按签约年度统计", stats.getYearStats(), headerStyle);
            rowIndex = writeBlock(sheet, rowIndex, "按金额区间统计", stats.getAmountRangeStats(), headerStyle);
            rowIndex = writeBlock(sheet, rowIndex, "按合作单位统计", stats.getPartnerStats(), headerStyle);
            finishSheet(sheet, 4, rowIndex);

            Sheet detail = workbook.createSheet("合同明细");
            String[] headers = {"序号", "合同编号", "合同名称", "甲方", "乙方", "签订日期", "到期日期",
                    "合同金额(元)", "合同类型", "归属部门", "负责人", "状态", "归档编号"};
            createHeader(detail, headers, headerStyle);
            int detailRow = 1;
            for (ContractVO c : details) {
                Row row = detail.createRow(detailRow);
                int col = 0;
                row.createCell(col++).setCellValue(detailRow);
                row.createCell(col++).setCellValue(nvl(c.getContractNo()));
                row.createCell(col++).setCellValue(nvl(c.getContractName()));
                row.createCell(col++).setCellValue(nvl(c.getPartyA()));
                row.createCell(col++).setCellValue(nvl(c.getPartyB()));
                row.createCell(col++).setCellValue(date(c.getSignDate()));
                row.createCell(col++).setCellValue(date(c.getExpireDate()));
                row.createCell(col++).setCellValue(c.getAmount() == null ? 0 : c.getAmount().doubleValue());
                row.createCell(col++).setCellValue(nvl(c.getContractTypeName()));
                row.createCell(col++).setCellValue(nvl(c.getDeptName()));
                row.createCell(col++).setCellValue(nvl(c.getOwnerName()));
                row.createCell(col++).setCellValue(nvl(c.getStatusName()));
                row.createCell(col).setCellValue(nvl(c.getArchiveNo()));
                detailRow++;
            }
            finishSheet(detail, headers.length, detailRow);
            write(workbook, response, "合同档案统计分析报表.xlsx");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("报表导出失败：" + e.getMessage());
        }
    }

    public void downloadImportTemplate(HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("合同导入");
            createHeader(sheet, CONTRACT_HEADERS, headerStyle(workbook));
            CellStyle hintStyle = workbook.createCellStyle();
            hintStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            hintStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            hintStyle.setWrapText(true);
            sheet.setColumnWidth(0, 18 * 256);
            sheet.setColumnWidth(1, 26 * 256);
            sheet.setColumnWidth(2, 22 * 256);
            sheet.setColumnWidth(3, 22 * 256);
            sheet.setColumnWidth(7, 16 * 256);
            sheet.setColumnWidth(12, 30 * 256);
            Row hint = sheet.createRow(1);
            Cell hintCell = hint.createCell(0);
            hintCell.setCellValue("说明：带*为必填。日期格式 yyyy-MM-dd。合同类型/密级填写字典名称。导入后为草稿状态，请逐个补充附件后提交归档。单次最多导入1000条。");
            hintCell.setCellStyle(hintStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 12));

            Sheet ref = workbook.createSheet("填写参考");
            int row = 0;
            row = writeRefBlock(ref, row, "合同类型", dictService.items("contract_type"));
            row = writeRefBlock(ref, row, "密级", dictService.items("security_level"));
            row += 1;
            ref.createRow(row++).createCell(0).setCellValue("归属部门（填写部门名称）");
            for (SysDept dept : deptService.flatList()) {
                ref.createRow(row++).createCell(0).setCellValue(dept.getDeptName());
            }
            ref.setColumnWidth(0, 30 * 256);
            ref.setColumnWidth(1, 30 * 256);
            write(workbook, response, "合同批量导入模板.xlsx");
        } catch (Exception e) {
            throw new BizException("模板生成失败");
        }
    }

    private int writeRefBlock(Sheet sheet, int row, String title, List<SysDictItem> items) {
        Row header = sheet.createRow(row++);
        header.createCell(0).setCellValue(title + "（填写名称）");
        header.createCell(1).setCellValue("系统值");
        for (SysDictItem item : items) {
            Row r = sheet.createRow(row++);
            r.createCell(0).setCellValue(item.getItemLabel());
            r.createCell(1).setCellValue(item.getItemValue());
        }
        return row + 1;
    }

    public ImportResultVO importContracts(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要导入的Excel文件");
        }
        String name = Objects.requireNonNullElse(file.getOriginalFilename(), "");
        if (!name.toLowerCase(Locale.ROOT).endsWith(".xlsx") && !name.toLowerCase(Locale.ROOT).endsWith(".xls")) {
            throw new BizException("仅支持 .xlsx 或 .xls 格式的Excel文件");
        }
        ImportResultVO result = new ImportResultVO();
        Map<String, String> typeLabels = reverseLabelMap("contract_type");
        Map<String, String> securityLabels = reverseLabelMap("security_level");
        Map<String, SysDept> deptMap = new HashMap<>();
        deptService.flatList().forEach(d -> {
            deptMap.put(d.getDeptName(), d);
            deptMap.put(d.getDeptCode(), d);
        });
        Map<String, SysUser> ownerMap = new HashMap<>();
        userService.listAllSimple().forEach(u -> {
            ownerMap.put(u.getRealName(), u);
            ownerMap.put(u.getUsername(), u);
        });

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BizException("Excel中没有可读取的工作表");
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BizException("导入模板表头缺失");
            }
            Map<String, Integer> headerIndex = new HashMap<>();
            DataFormatter formatter = new DataFormatter();
            for (Cell cell : headerRow) {
                headerIndex.put(formatter.formatCellValue(cell).replace("*", "").trim(), cell.getColumnIndex());
            }
            for (String required : new String[]{"合同编号", "合同名称", "甲方", "乙方", "签订日期", "合同金额(元)",
                    "合同类型", "归属部门", "负责人", "密级"}) {
                if (!headerIndex.containsKey(required)) {
                    throw new BizException("导入模板缺少列：" + required);
                }
            }
            int success = 0;
            int fail = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                if (success + fail >= 1000) {
                    result.getMessages().add("单次最多导入1000条，超出部分已忽略");
                    break;
                }
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }
                String contractNo = getString(row, headerIndex.get("合同编号"), formatter);
                // 模板第 2 行为合并单元格的填写说明，合并区域的文字会落在合同编号列，需直接跳过。
                if (contractNo.startsWith("说明")) {
                    continue;
                }
                try {
                    ContractSaveRequest request = new ContractSaveRequest();
                    request.setContractNo(contractNo);
                    request.setContractName(getString(row, headerIndex.get("合同名称"), formatter));
                    request.setPartyA(getString(row, headerIndex.get("甲方"), formatter));
                    request.setPartyB(getString(row, headerIndex.get("乙方"), formatter));
                    request.setSignDate(getDate(row, headerIndex.get("签订日期"), formatter));
                    request.setEffectiveDate(getDate(row, headerIndex.get("生效日期"), formatter));
                    request.setExpireDate(getDate(row, headerIndex.get("到期日期"), formatter));
                    request.setAmount(getAmount(row, headerIndex.get("合同金额(元)"), formatter));
                    String type = getString(row, headerIndex.get("合同类型"), formatter);
                    String security = getString(row, headerIndex.get("密级"), formatter);
                    request.setContractType(typeLabels.getOrDefault(type, type));
                    request.setSecurityLevel(securityLabels.getOrDefault(security, security));
                    String deptName = getString(row, headerIndex.get("归属部门"), formatter);
                    SysDept dept = deptMap.get(deptName);
                    if (dept == null) {
                        throw new BizException("归属部门【" + deptName + "】不存在");
                    }
                    request.setDeptId(dept.getId());
                    String ownerName = getString(row, headerIndex.get("负责人"), formatter);
                    SysUser owner = ownerMap.get(ownerName);
                    if (owner == null) {
                        throw new BizException("负责人【" + ownerName + "】不存在或已禁用");
                    }
                    request.setOwnerId(owner.getId());
                    request.setRemark(headerIndex.containsKey("备注")
                            ? getString(row, headerIndex.get("备注"), formatter) : "");
                    if (!StringUtils.hasText(request.getContractName()) || !StringUtils.hasText(request.getPartyA())
                            || !StringUtils.hasText(request.getPartyB()) || request.getSignDate() == null
                            || request.getAmount() == null || !StringUtils.hasText(request.getContractType())
                            || !StringUtils.hasText(request.getSecurityLevel())) {
                        throw new BizException("必填项不完整");
                    }
                    Long id = contractService.save(request);
                    result.getContractIds().add(id);
                    success++;
                } catch (Exception e) {
                    fail++;
                    String message = e instanceof BizException ? e.getMessage() : "数据格式错误";
                    result.getMessages().add("第" + (i + 1) + "行：" + message);
                }
            }
            result.setSuccessCount(success);
            result.setFailCount(fail);
            if (success == 0 && fail == 0) {
                throw new BizException("未读取到可导入的数据");
            }
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("Excel解析失败：" + e.getMessage());
        }
    }

    private Map<String, String> reverseLabelMap(String code) {
        Map<String, String> map = new HashMap<>();
        for (SysDictItem item : dictService.items(code)) {
            map.put(item.getItemLabel(), item.getItemValue());
            map.put(item.getItemValue(), item.getItemValue());
        }
        return map;
    }

    private int writeSummary(Sheet sheet, int rowIndex, String title, String[] headers, CellStyle headerStyle) {
        Row titleRow = sheet.createRow(rowIndex++);
        Cell cell = titleRow.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(titleStyle(sheet.getWorkbook()));
        for (int i = 0; i < headers.length; i++) {
            Cell headerCell = sheet.createRow(rowIndex).createCell(i);
            headerCell.setCellValue(headers[i]);
            headerCell.setCellStyle(headerStyle);
        }
        return rowIndex + 1;
    }

    private int writeBlock(Sheet sheet, int rowIndex, String title, List<StatItemVO> items, CellStyle headerStyle) {
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle(sheet.getWorkbook()));
        Row header = sheet.createRow(rowIndex++);
        String[] names = {"分组名称", "合同份数", "合同金额(元)"};
        for (int i = 0; i < names.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(names[i]);
            cell.setCellStyle(headerStyle);
        }
        for (StatItemVO item : items) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getName());
            row.createCell(1).setCellValue(item.getCount() == null ? 0 : item.getCount());
            row.createCell(2).setCellValue(item.getAmount() == null ? 0 : item.getAmount().doubleValue());
        }
        return rowIndex + 2;
    }

    private void writePair(Sheet sheet, int rowIndex, String key, String value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value);
    }

    private CellStyle titleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private void createHeader(Sheet sheet, String[] headers, CellStyle style) {
        Row header = sheet.createRow(0);
        header.setHeightInPoints(24);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void finishSheet(Sheet sheet, int columns, int rows) {
        sheet.createFreezePane(0, 1);
        if (rows > 1) {
            sheet.setAutoFilter(new CellRangeAddress(0, rows - 1, 0, Math.max(0, columns - 1)));
        }
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            if (width > 45 * 256) {
                sheet.setColumnWidth(i, 45 * 256);
            }
            if (width < 10 * 256) {
                sheet.setColumnWidth(i, 10 * 256);
            }
        }
    }

    private void write(Workbook workbook, HttpServletResponse response, String fileName) throws Exception {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        workbook.write(response.getOutputStream());
        response.flushBuffer();
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            if (StringUtils.hasText(formatter.formatCellValue(row.getCell(i)))) {
                return false;
            }
        }
        return true;
    }

    private String getString(Row row, Integer index, DataFormatter formatter) {
        if (index == null) return "";
        Cell cell = row.getCell(index);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private LocalDate getDate(Row row, Integer index, DataFormatter formatter) {
        if (index == null) return null;
        Cell cell = row.getCell(index);
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            String value = formatter.formatCellValue(cell).trim();
            if (!StringUtils.hasText(value)) return null;
            value = value.replace("/", "-").replace(".", "-");
            if (value.matches("\\d{8}")) {
                return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
            }
            return LocalDate.parse(value.substring(0, 10));
        } catch (Exception e) {
            throw new BizException("日期格式错误：" + formatter.formatCellValue(cell));
        }
    }

    private BigDecimal getAmount(Row row, Integer index, DataFormatter formatter) {
        if (index == null) return null;
        Cell cell = row.getCell(index);
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            String value = formatter.formatCellValue(cell).replace(",", "").replace("元", "").trim();
            return new BigDecimal(value);
        } catch (Exception e) {
            throw new BizException("合同金额格式错误");
        }
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private String date(LocalDate value) {
        return value == null ? "" : value.toString();
    }
}
