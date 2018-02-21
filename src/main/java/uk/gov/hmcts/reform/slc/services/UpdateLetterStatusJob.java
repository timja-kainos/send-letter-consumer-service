package uk.gov.hmcts.reform.slc.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.slc.services.steps.sftpupload.FtpClient;

import java.util.List;

import static java.time.LocalTime.now;

@Component
public class UpdateLetterStatusJob {

    private static final Logger logger = LoggerFactory.getLogger(UpdateLetterStatusJob.class);

    private final FtpClient ftpClient;
    private final FtpAvailabilityChecker ftpAvailabilityChecker;
    private final SendLetterClient sendLetterClient;
    private final ReportParser parser;


    public UpdateLetterStatusJob(
        FtpClient ftpClient,
        FtpAvailabilityChecker ftpAvailabilityChecker,
        SendLetterClient sendLetterClient,
        ReportParser parser
    ) {
        this.ftpClient = ftpClient;
        this.ftpAvailabilityChecker = ftpAvailabilityChecker;
        this.sendLetterClient = sendLetterClient;
        this.parser = parser;
    }

    @Scheduled(cron = "${reports-cron}")
    public void run() {
        logger.trace("Running job");

        if (ftpAvailabilityChecker.isFtpAvailable(now())) {
            ftpClient
                .downloadReports()
                .stream()
                .map(parser::parse)
                .flatMap(List::stream)
                .forEach(sendLetterClient::updatePrintedAt);
        } else {
            logger.trace("FTP server not available, job cancelled");
        }
    }
}