package uk.gov.hmcts.reform.slc.services;

import com.microsoft.azure.servicebus.IMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.slc.model.Letter;
import uk.gov.hmcts.reform.slc.services.servicebus.MessageHandlingResult;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.PdfCreator;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.PdfDoc;
import uk.gov.hmcts.reform.slc.services.steps.maptoletter.LetterMapper;
import uk.gov.hmcts.reform.slc.services.steps.sftpupload.FtpClient;

import static uk.gov.hmcts.reform.slc.services.servicebus.MessageHandlingResult.FAILURE;
import static uk.gov.hmcts.reform.slc.services.servicebus.MessageHandlingResult.SUCCESS;

@Service
public class SendLetterService {

    private static final Logger logger = LoggerFactory.getLogger(SendLetterService.class);

    private final LetterMapper letterMapper;
    private final PdfCreator pdfCreator;
    private final FtpClient ftpClient;
    private final SendLetterClient sendLetterClient;

    public SendLetterService(
        LetterMapper letterMapper,
        PdfCreator pdfCreator,
        FtpClient ftpClient,
        SendLetterClient sendLetterClient
    ) {
        this.letterMapper = letterMapper;
        this.pdfCreator = pdfCreator;
        this.ftpClient = ftpClient;
        this.sendLetterClient = sendLetterClient;
    }

    public MessageHandlingResult send(IMessage msg) {
        try {
            Letter letter = letterMapper.from(msg);
            PdfDoc pdf = pdfCreator.create(letter);
            // TODO: encrypt & sign
            ftpClient.upload(pdf);

            //update producer with sent to print at time for reporting
            sendLetterClient.updateSentToPrintAt(letter.id);

            return SUCCESS;

        } catch (Exception exc) {
            logger.error(exc.getMessage(), exc.getCause());

            return FAILURE;
        }
    }
}
