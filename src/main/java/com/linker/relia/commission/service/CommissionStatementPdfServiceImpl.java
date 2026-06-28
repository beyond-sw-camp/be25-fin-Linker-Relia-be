package com.linker.relia.commission.service;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.commission.domain.BranchIncomeCommissionMonthlyClosing;
import com.linker.relia.commission.domain.FpCommissionMonthlyClosing;
import com.linker.relia.commission.domain.IncomeCommissionMonthlyClosing;
import com.linker.relia.commission.dto.FpCommissionListQueryResult;
import com.linker.relia.commission.dto.OrganizationCommissionListQueryResult;
import com.linker.relia.commission.repository.BranchCommissionMonthlyClosingRepository;
import com.linker.relia.commission.repository.BranchIncomeCommissionMonthlyClosingRepository;
import com.linker.relia.commission.repository.FpCommissionMonthlyClosingRepository;
import com.linker.relia.commission.repository.IncomeCommissionMonthlyClosingRepository;
import com.linker.relia.common.access.AccessScope;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.common.exception.CommonErrorCode;
import com.linker.relia.organization.domain.Organization;
import com.linker.relia.security.principal.PrincipalDetails;
import com.linker.relia.user.domain.User;
import com.linker.relia.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommissionStatementPdfServiceImpl implements CommissionStatementPdfService {
    private static final DateTimeFormatter ISSUED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final CommissionAccessService commissionAccessService;
    private final IncomeCommissionMonthlyClosingRepository incomeCommissionMonthlyClosingRepository;
    private final BranchIncomeCommissionMonthlyClosingRepository branchIncomeCommissionMonthlyClosingRepository;
    private final BranchCommissionMonthlyClosingRepository branchCommissionMonthlyClosingRepository;
    private final FpCommissionMonthlyClosingRepository fpCommissionMonthlyClosingRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] generateHqStatement(PrincipalDetails principalDetails, String closingMonth) {
        AccessScope accessScope = commissionAccessService.resolveAccessScope(principalDetails);
        if (!accessScope.isAllScope()) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
        }

        String normalizedClosingMonth = normalizeClosingMonth(closingMonth);
        IncomeCommissionMonthlyClosing current = incomeCommissionMonthlyClosingRepository
                .findByClosingMonth(normalizedClosingMonth)
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.INVALID_REQUEST,
                        "해당 정산월의 전사 수수료 마감 데이터가 없습니다."
                ));
        IncomeCommissionMonthlyClosing previous = incomeCommissionMonthlyClosingRepository
                .findByClosingMonth(YearMonth.parse(normalizedClosingMonth).minusMonths(1).toString())
                .orElse(null);

        StatementSummary summary = StatementSummary.hq(current, previous);
        List<OrganizationCommissionListQueryResult> organizationRows =
                branchCommissionMonthlyClosingRepository.findHqOrganizationCommissionStatementRows(normalizedClosingMonth);
        List<FpCommissionListQueryResult> fpRows =
                fpCommissionMonthlyClosingRepository.findHqFpCommissionStatementRows(normalizedClosingMonth);

        return renderOrganizationStatement(
                "전체 지점 수수료 명세서",
                normalizedClosingMonth,
                "전체 지점",
                principalDetails.getUser().getUserName(),
                summary,
                organizationRows,
                fpRows
        );
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateBranchStatement(PrincipalDetails principalDetails,
                                          String closingMonth,
                                          String organizationCode) {
        AccessScope accessScope = commissionAccessService.resolveAccessScope(principalDetails);
        String normalizedClosingMonth = normalizeClosingMonth(closingMonth);
        Organization organization = resolveReadableOrganization(principalDetails, accessScope, organizationCode);

        BranchIncomeCommissionMonthlyClosing current = branchIncomeCommissionMonthlyClosingRepository
                .findByOrganization_IdAndClosingMonth(organization.getId(), normalizedClosingMonth)
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.INVALID_REQUEST,
                        "해당 정산월의 지점 수수료 마감 데이터가 없습니다."
                ));
        BranchIncomeCommissionMonthlyClosing previous = branchIncomeCommissionMonthlyClosingRepository
                .findByOrganization_IdAndClosingMonth(
                        organization.getId(),
                        YearMonth.parse(normalizedClosingMonth).minusMonths(1).toString()
                )
                .orElse(null);

        StatementSummary summary = StatementSummary.branch(current, previous);
        List<FpCommissionListQueryResult> fpRows =
                fpCommissionMonthlyClosingRepository.findBranchFpCommissionStatementRows(
                        normalizedClosingMonth,
                        organization.getId()
                );

        return renderOrganizationStatement(
                "지점 수수료 명세서",
                normalizedClosingMonth,
                organization.getOrganizationName(),
                principalDetails.getUser().getUserName(),
                summary,
                List.of(),
                fpRows
        );
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateFpStatement(PrincipalDetails principalDetails, String closingMonth, UUID fpId) {
        AccessScope accessScope = commissionAccessService.resolveAccessScope(principalDetails);
        String normalizedClosingMonth = normalizeClosingMonth(closingMonth);
        User fp = userRepository.findByIdAndDeletedAtIsNull(fpId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST, "설계사를 찾을 수 없습니다."));

        validateFpReadable(accessScope, fp);

        FpCommissionMonthlyClosing current = fpCommissionMonthlyClosingRepository
                .findByFp_IdAndClosingMonth(fpId, normalizedClosingMonth)
                .orElseThrow(() -> new BusinessException(
                        CommonErrorCode.INVALID_REQUEST,
                        "해당 정산월의 설계사 수수료 마감 데이터가 없습니다."
                ));

        StatementSummary summary = StatementSummary.fp(current);
        FpCommissionListQueryResult fpRow = new FpCommissionListQueryResult(
                fp.getId(),
                fp.getUserName(),
                current.getTotalInitialPaymentAmount(),
                current.getTotalMaintenancePaymentAmount(),
                current.getTotalRecoveryCollectionAmount(),
                current.getTotalPaymentAmount(),
                current.getNetCommissionAmount(),
                current.getContractCount(),
                current.getRecoveryContractCount()
        );

        return renderFpStatement(
                normalizedClosingMonth,
                fp,
                principalDetails.getUser().getUserName(),
                summary,
                fpRow
        );
    }

    private String normalizeClosingMonth(String closingMonth) {
        if (closingMonth == null || closingMonth.isBlank()) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "closingMonth는 필수입니다.");
        }

        try {
            return YearMonth.parse(closingMonth.trim()).toString();
        } catch (Exception exception) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "closingMonth는 YYYY-MM 형식이어야 합니다.");
        }
    }

    private Organization resolveReadableOrganization(PrincipalDetails principalDetails,
                                                    AccessScope accessScope,
                                                    String organizationCode) {
        if (organizationCode == null || organizationCode.isBlank()) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "organizationCode는 필수입니다.");
        }

        if (accessScope.isBranchScope()) {
            Organization ownOrganization = principalDetails.getUser().getOrganization();
            if (ownOrganization == null || !organizationCode.equals(ownOrganization.getOrganizationCode())) {
                throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
            }
            return ownOrganization;
        }

        if (accessScope.isAllScope()) {
            return commissionAccessService.resolveOrganization(organizationCode);
        }

        throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
    }

    private void validateFpReadable(AccessScope accessScope, User fp) {
        if (accessScope.isOwnScope()) {
            if (!accessScope.userId().equals(fp.getId())) {
                throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
            }
            return;
        }

        if (accessScope.isBranchScope()) {
            Organization fpOrganization = fp.getOrganization();
            if (fpOrganization == null || !accessScope.organizationId().equals(fpOrganization.getId())) {
                throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
            }
        }
    }

    private byte[] renderOrganizationStatement(String title,
                                               String closingMonth,
                                               String scopeName,
                                               String issuerName,
                                               StatementSummary summary,
                                               List<OrganizationCommissionListQueryResult> organizationRows,
                                               List<FpCommissionListQueryResult> fpRows) {
        try (PDDocument document = new PDDocument()) {
            PdfWriter writer = new PdfWriter(document);
            writer.header(title, closingMonth, scopeName, issuerName);
            writer.summaryCards(summary, true);
            writer.calculation(summary);

            if (!organizationRows.isEmpty()) {
                writer.organizationTable(organizationRows);
            }
            writer.fpTable(fpRows);
            writer.notice();
            return writer.toBytes();
        } catch (IOException exception) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "수수료 명세서 PDF 생성에 실패했습니다.");
        }
    }

    private byte[] renderFpStatement(String closingMonth,
                                     User fp,
                                     String issuerName,
                                     StatementSummary summary,
                                     FpCommissionListQueryResult fpRow) {
        String scopeName = fp.getOrganization() == null
                ? fp.getUserName()
                : fp.getOrganization().getOrganizationName() + " / " + fp.getUserName();

        try (PDDocument document = new PDDocument()) {
            PdfWriter writer = new PdfWriter(document);
            writer.header("설계사 개인 수수료 명세서", closingMonth, scopeName, issuerName);
            writer.summaryCards(summary, false);
            writer.fpCalculation(summary);
            writer.fpTable(List.of(fpRow));
            writer.notice();
            return writer.toBytes();
        } catch (IOException exception) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "설계사 수수료 명세서 PDF 생성에 실패했습니다.");
        }
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record StatementSummary(
            BigDecimal totalPaymentCommissionAmount,
            BigDecimal incomeCommissionAmount,
            BigDecimal incomeChangeRate,
            BigDecimal previousIncomeCommissionAmount,
            BigDecimal initialCommissionAmount,
            BigDecimal maintenanceCommissionAmount,
            BigDecimal insuranceRecoveryAmount,
            BigDecimal fpRecoveryAmount,
            BigDecimal recoveryNetLossAmount
    ) {
        private static StatementSummary hq(IncomeCommissionMonthlyClosing current,
                                           IncomeCommissionMonthlyClosing previous) {
            return new StatementSummary(
                    nvl(current.getTotalPaymentCommissionAmount()),
                    nvl(current.getNetIncomeCommissionAmount()),
                    changeRate(current.getNetIncomeCommissionAmount(),
                            previous == null ? null : previous.getNetIncomeCommissionAmount()),
                    previous == null ? BigDecimal.ZERO : nvl(previous.getNetIncomeCommissionAmount()),
                    nvl(current.getTotalInitialGrossCommissionAmount()),
                    nvl(current.getTotalMaintenanceGrossCommissionAmount()),
                    nvl(current.getTotalInsuranceRecoveryAmount()),
                    nvl(current.getTotalFpRecoveryCollectionAmount()),
                    nvl(current.getTotalFpRecoveryCollectionAmount()).subtract(nvl(current.getTotalInsuranceRecoveryAmount()))
            );
        }

        private static StatementSummary branch(BranchIncomeCommissionMonthlyClosing current,
                                               BranchIncomeCommissionMonthlyClosing previous) {
            return new StatementSummary(
                    nvl(current.getTotalPaymentCommissionAmount()),
                    nvl(current.getNetIncomeCommissionAmount()),
                    changeRate(current.getNetIncomeCommissionAmount(),
                            previous == null ? null : previous.getNetIncomeCommissionAmount()),
                    previous == null ? BigDecimal.ZERO : nvl(previous.getNetIncomeCommissionAmount()),
                    nvl(current.getTotalInitialGrossCommissionAmount()),
                    nvl(current.getTotalMaintenanceGrossCommissionAmount()),
                    nvl(current.getTotalInsuranceRecoveryAmount()),
                    nvl(current.getTotalFpRecoveryCollectionAmount()),
                    nvl(current.getTotalFpRecoveryCollectionAmount()).subtract(nvl(current.getTotalInsuranceRecoveryAmount()))
            );
        }

        private static StatementSummary fp(FpCommissionMonthlyClosing current) {
            return new StatementSummary(
                    nvl(current.getTotalPaymentAmount()),
                    nvl(current.getNetCommissionAmount()),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    nvl(current.getTotalInitialPaymentAmount()),
                    nvl(current.getTotalMaintenancePaymentAmount()),
                    BigDecimal.ZERO,
                    nvl(current.getTotalRecoveryCollectionAmount()),
                    nvl(current.getTotalRecoveryCollectionAmount()).negate()
            );
        }

        private static BigDecimal changeRate(BigDecimal current, BigDecimal previous) {
            if (previous == null || BigDecimal.ZERO.compareTo(previous) == 0) {
                return BigDecimal.ZERO;
            }
            return nvl(current).subtract(previous)
                    .multiply(ONE_HUNDRED)
                    .divide(previous, 1, RoundingMode.HALF_UP);
        }
    }

    private static final class PdfWriter {
        private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
        private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
        private static final float MARGIN = 42;
        private static final float LINE = 15;
        private static final float TABLE_ROW = 24;

        private final PDDocument document;
        private final PDFont regularFont;
        private final PDFont boldFont;
        private PDPage page;
        private PDPageContentStream content;
        private float y;
        private int pageNumber = 0;

        private PdfWriter(PDDocument document) throws IOException {
            this.document = document;
            this.regularFont = loadFont(document, false);
            this.boldFont = loadFont(document, true);
            newPage();
        }

        private void header(String title, String closingMonth, String scopeName, String issuerName) throws IOException {
            text("Relia", MARGIN, y, 18, boldFont);
            text(title, MARGIN, y - 28, 22, boldFont);
            text("정산월: " + closingMonth + "   조회 범위: " + scopeName, MARGIN, y - 52, 10, regularFont);
            text("발급자: " + issuerName + "   발급일시: " + LocalDateTime.now().format(ISSUED_AT_FORMATTER),
                    MARGIN, y - 68, 10, regularFont);
            line(MARGIN, y - 84, PAGE_WIDTH - MARGIN, y - 84);
            y -= 110;
        }

        private void summaryCards(StatementSummary summary, boolean organizationStatement) throws IOException {
            section("핵심 요약");
            float cardWidth = (PAGE_WIDTH - MARGIN * 2 - 12) / 2;
            card("총 지급 수수료", money(summary.totalPaymentCommissionAmount()),
                    organizationStatement ? "설계사 지급 총액" : "개인 지급 예정 총액",
                    MARGIN, y, cardWidth);
            card(organizationStatement ? "수입 수수료" : "실수령 수수료", money(summary.incomeCommissionAmount()),
                    organizationStatement ? "회사 관점 순수입" : "지급 예정액에서 환수 차감 후 금액",
                    MARGIN + cardWidth + 12, y, cardWidth);
            y -= 82;

            card("신계약 수수료", money(summary.initialCommissionAmount()), "신규 계약에서 발생",
                    MARGIN, y, cardWidth);
            card("유지 수수료", money(summary.maintenanceCommissionAmount()), "유지 계약에서 발생",
                    MARGIN + cardWidth + 12, y, cardWidth);
            y -= 82;

            if (organizationStatement) {
                card("원수사 환수금", money(summary.insuranceRecoveryAmount()), "보험사가 GA에 청구한 환수",
                        MARGIN, y, cardWidth);
                card("설계사 환수금", money(summary.fpRecoveryAmount()), "회사가 설계사에게 회수한 금액",
                        MARGIN + cardWidth + 12, y, cardWidth);
                y -= 82;
            }

            card(organizationStatement ? "환수 순손실" : "환수 차감액",
                    money(summary.recoveryNetLossAmount()),
                    organizationStatement ? "설계사 환수금 - 원수사 환수금" : "설계사 환수 차감 금액",
                    MARGIN, y, cardWidth);
            String previousText = organizationStatement
                    ? "전월 " + money(summary.previousIncomeCommissionAmount()) + " 대비"
                    : "개인 명세서는 전월 증감률 미표시";
            card("전월 대비 증감률", percent(summary.incomeChangeRate()), previousText,
                    MARGIN + cardWidth + 12, y, cardWidth);
            y -= 96;
        }

        private void calculation(StatementSummary summary) throws IOException {
            section("수수료 산출 구조");
            box(MARGIN, y - 112, PAGE_WIDTH - MARGIN * 2, 96);
            text("회사 발생 수수료", MARGIN + 14, y - 34, 11, boldFont);
            text(money(summary.initialCommissionAmount()) + " + " + money(summary.maintenanceCommissionAmount())
                            + " = " + money(summary.initialCommissionAmount().add(summary.maintenanceCommissionAmount())),
                    MARGIN + 130, y - 34, 11, regularFont);
            text("차감 항목", MARGIN + 14, y - 58, 11, boldFont);
            text("설계사 지급 " + money(summary.totalPaymentCommissionAmount())
                            + ", 환수 순손실 " + money(summary.recoveryNetLossAmount().abs()),
                    MARGIN + 130, y - 58, 11, regularFont);
            text("계산식", MARGIN + 14, y - 82, 11, boldFont);
            text(money(summary.initialCommissionAmount().add(summary.maintenanceCommissionAmount()))
                            + " - " + money(summary.totalPaymentCommissionAmount())
                            + " - " + money(summary.recoveryNetLossAmount().abs())
                            + " = " + money(summary.incomeCommissionAmount()),
                    MARGIN + 130, y - 82, 11, regularFont);
            y -= 132;
        }

        private void fpCalculation(StatementSummary summary) throws IOException {
            section("개인 지급 산출 구조");
            box(MARGIN, y - 88, PAGE_WIDTH - MARGIN * 2, 72);
            text("지급 예정액", MARGIN + 14, y - 34, 11, boldFont);
            text(money(summary.initialCommissionAmount()) + " + " + money(summary.maintenanceCommissionAmount())
                            + " = " + money(summary.totalPaymentCommissionAmount()),
                    MARGIN + 130, y - 34, 11, regularFont);
            text("실수령액", MARGIN + 14, y - 58, 11, boldFont);
            text(money(summary.totalPaymentCommissionAmount()) + " - " + money(summary.fpRecoveryAmount())
                            + " = " + money(summary.incomeCommissionAmount()),
                    MARGIN + 130, y - 58, 11, regularFont);
            y -= 108;
        }

        private void organizationTable(List<OrganizationCommissionListQueryResult> rows) throws IOException {
            section("지점별 월 수수료 현황");
            tableHeader(new String[]{"지점명", "FP", "계약", "환수", "지급 예정", "환수 차감", "최종 지급"},
                    new float[]{96, 42, 42, 42, 94, 94, 94});
            for (OrganizationCommissionListQueryResult row : rows) {
                ensureSpace(TABLE_ROW + 24);
                tableRow(new String[]{
                                row.getOrganizationName(),
                                String.valueOf(row.getFpCount()),
                                String.valueOf(row.getContractCount()),
                                String.valueOf(row.getRecoveryContractCount()),
                                money(row.getTotalPaymentCommissionAmount()),
                                money(row.getRecoveryAmount()),
                                money(row.getNetCommissionAmount())
                        },
                        new float[]{96, 42, 42, 42, 94, 94, 94});
            }
            y -= 20;
        }

        private void fpTable(List<FpCommissionListQueryResult> rows) throws IOException {
            section("설계사별 월 수수료 현황");
            tableHeader(new String[]{"설계사명", "계약", "환수", "신계약", "유지", "지급 예정", "환수 차감", "실수령"},
                    new float[]{68, 34, 34, 68, 68, 76, 76, 76});
            for (FpCommissionListQueryResult row : rows) {
                ensureSpace(TABLE_ROW + 24);
                tableRow(new String[]{
                                row.getFpName(),
                                String.valueOf(row.getContractCount()),
                                String.valueOf(row.getRecoveryContractCount()),
                                money(row.getInitialCommissionAmount()),
                                money(row.getMaintenanceCommissionAmount()),
                                money(row.getTotalPaymentCommissionAmount()),
                                money(row.getRecoveryAmount()),
                                money(row.getNetCommissionAmount())
                        },
                        new float[]{68, 34, 34, 68, 68, 76, 76, 76});
            }
            y -= 20;
        }

        private void notice() throws IOException {
            ensureSpace(70);
            section("안내");
            text("본 명세서는 정산월 마감 데이터 기준으로 생성되었습니다.", MARGIN, y, 10, regularFont);
            text("지급 및 환수 금액은 정정 처리에 따라 변경될 수 있습니다.", MARGIN, y - LINE, 10, regularFont);
        }

        private void section(String title) throws IOException {
            ensureSpace(42);
            text(title, MARGIN, y, 14, boldFont);
            y -= 24;
        }

        private void card(String label, String value, String description, float x, float topY, float width)
                throws IOException {
            box(x, topY - 64, width, 58);
            text(label, x + 12, topY - 23, 9, regularFont);
            text(value, x + 12, topY - 43, 16, boldFont);
            text(description, x + 12, topY - 57, 8, regularFont);
        }

        private void tableHeader(String[] values, float[] widths) throws IOException {
            ensureSpace(TABLE_ROW * 2);
            fillBox(MARGIN, y - TABLE_ROW + 5, PAGE_WIDTH - MARGIN * 2, TABLE_ROW, 0.94f);
            tableRow(values, widths, boldFont);
        }

        private void tableRow(String[] values, float[] widths) throws IOException {
            tableRow(values, widths, regularFont);
        }

        private void tableRow(String[] values, float[] widths, PDFont font) throws IOException {
            float x = MARGIN;
            line(MARGIN, y - TABLE_ROW + 2, PAGE_WIDTH - MARGIN, y - TABLE_ROW + 2);
            for (int i = 0; i < values.length; i++) {
                text(fit(values[i], widths[i] - 8, 8, font), x + 4, y - 15, 8, font);
                x += widths[i];
            }
            y -= TABLE_ROW;
        }

        private void ensureSpace(float height) throws IOException {
            if (y - height < MARGIN + 24) {
                newPage();
            }
        }

        private void newPage() throws IOException {
            if (content != null) {
                footer();
                content.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            pageNumber++;
            y = PAGE_HEIGHT - MARGIN;
        }

        private void footer() throws IOException {
            text("Page " + pageNumber, PAGE_WIDTH - MARGIN - 45, MARGIN - 18, 8, regularFont);
        }

        private byte[] toBytes() throws IOException {
            footer();
            content.close();
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                document.save(outputStream);
                return outputStream.toByteArray();
            }
        }

        private void text(String text, float x, float y, float size, PDFont font) throws IOException {
            content.beginText();
            content.setFont(font, size);
            content.newLineAtOffset(x, y);
            content.showText(Objects.toString(text, ""));
            content.endText();
        }

        private void line(float x1, float y1, float x2, float y2) throws IOException {
            content.moveTo(x1, y1);
            content.lineTo(x2, y2);
            content.stroke();
        }

        private void box(float x, float y, float width, float height) throws IOException {
            content.addRect(x, y, width, height);
            content.stroke();
        }

        private void fillBox(float x, float y, float width, float height, float gray) throws IOException {
            content.setNonStrokingColor(gray);
            content.addRect(x, y, width, height);
            content.fill();
            content.setNonStrokingColor(0f);
        }

        private String fit(String value, float maxWidth, int fontSize, PDFont font) throws IOException {
            String text = Objects.toString(value, "");
            if (font.getStringWidth(text) / 1000 * fontSize <= maxWidth) {
                return text;
            }
            String suffix = "...";
            while (!text.isEmpty() && font.getStringWidth(text + suffix) / 1000 * fontSize > maxWidth) {
                text = text.substring(0, text.length() - 1);
            }
            return text + suffix;
        }

        private static PDFont loadFont(PDDocument document, boolean bold) throws IOException {
            String resourcePath = bold ? "fonts/NanumGothicBold.ttf" : "fonts/NanumGothic.ttf";
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    return PDType0Font.load(document, inputStream);
                }
            }

            for (Path path : fontCandidates(bold)) {
                if (Files.exists(path)) {
                    try (InputStream inputStream = Files.newInputStream(path)) {
                        return PDType0Font.load(document, inputStream);
                    }
                }
            }

            throw new IOException("Korean font file was not found.");
        }

        private static List<Path> fontCandidates(boolean bold) {
            if (bold) {
                return List.of(
                        Path.of("/usr/share/fonts/truetype/nanum/NanumGothicBold.ttf"),
                        Path.of("C:/Windows/Fonts/NotoSansKR-VF.ttf"),
                        Path.of("C:/Windows/Fonts/malgunbd.ttf")
                );
            }

            return List.of(
                    Path.of("/usr/share/fonts/truetype/nanum/NanumGothic.ttf"),
                    Path.of("C:/Windows/Fonts/NotoSansKR-VF.ttf"),
                    Path.of("C:/Windows/Fonts/malgun.ttf")
            );
        }
    }

    private static String money(BigDecimal value) {
        BigDecimal normalized = nvl(value).setScale(0, RoundingMode.HALF_UP);
        return String.format("%,d원", normalized.longValue());
    }

    private static String percent(BigDecimal value) {
        return nvl(value).setScale(1, RoundingMode.HALF_UP) + "%";
    }
}
