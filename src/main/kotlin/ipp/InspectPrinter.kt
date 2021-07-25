package ipp

import de.gmuth.ipp.client.IppExchangeException
import de.gmuth.ipp.client.IppPrinter
import de.gmuth.ipp.client.IppTemplateAttributes.jobName
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppTag.Job
import de.gmuth.log.Logging
import java.io.File
import java.net.URI

/**
 * InspectPrinter requires one argument: the printerUri
 * It will exchange a few ipp requests and save the ipp responses returned by the printer in directory 'printers'
 * Operations:
 * - Get-Printer-Attributes
 * - Print-Job, Get-Jobs, Get-Job-Attributes
 * - Hold-Job, Release-Job, Cancel-Job
 */
fun main(args: Array<String>) {
    val log = Logging.getLogger { }
    try {
        var printerUri = URI.create("ipp://localhost:8632/printers/laser") // Apple's PrinterSimulator
        if (args.size > 0) printerUri = URI.create(args[0])

        var cancelJob = true

        log.info { "printerUri: $printerUri" }
        IppPrinter(printerUri, verifySSLHostname = false).run {

            val printerModel = with(StringBuilder()) {
                if (isCups()) append("CUPS_")
                append(makeAndModel.text.replace("\\s+".toRegex(), "_"))
                toString()
            }
            log.info { "printerModel: $printerModel" }

            val printerDirectory = File("printers", printerModel).apply {
                if (!isDirectory && !mkdirs()) throw RuntimeException("failed to create directory: $path")
            }

            attributes.run {
                if (containsKey("media-supported")) log.info { "media-supported: $mediaSupported" }
                if (containsKey("media-default")) log.info { "media-default: $mediaDefault" }
                if (containsKey("media-ready")) log.info { "media-ready: $mediaReady" }
            }

            val mediaReady = if (attributes.containsKey("media-ready")) mediaReady.first() else null
            log.info { "mediaReady: $mediaReady " }

            val pdfName = when (mediaReady) {
                "na_letter_8.5x11in" -> "blank_USLetter.pdf"
                "iso_a4_210x297mm" -> "blank_A4.pdf"
                null -> {
                    log.warn { "printer does not support 'media-ready', trying A4" }
                    "blank_A4.pdf"
                }
                else -> {
                    log.warn { "no pdf available for media '$mediaReady', trying A4" }
                    "blank_A4.pdf"
                }
            }
            log.info { "pdfName: $pdfName" }
            val pdfStream = javaClass.getResourceAsStream("/$pdfName")

            ippClient.responseInterceptor = { request, response ->
                response.saveRawBytes(File(printerDirectory, "${request.operation}.ipp"))
                when (request.operation) {
                    GetPrinterAttributes -> response.printerGroup
                    GetJobAttributes, PrintJob -> response.jobGroup
                    GetJobs -> with(response.getAttributesGroups(Job)) {
                        // supporting only first job
                        if (size > 0) first() else null
                    }
                    else -> null
                }?.run {
                    saveText(File(printerDirectory, "${request.operation}.txt"))
                }
            }

            log.info { "get printer attributes" }
            getPrinterAttributes()

            log.info { "print job pdfName" }
            printJob(pdfStream, jobName(pdfName)).run {
                log.info { toString() }

                log.info { "get jobs" }
                for (job in getJobs()) {
                    log.info { job }
                }

                if (supportsOperations(HoldJob, ReleaseJob)) {
                    log.info { "hold job" }
                    hold()
                    log.info { "get job attributes " }
                    getJobAttributes()
                    log.info { "release job" }
                    release()
                }

                if (cancelJob) {
                    log.info { "cancel job" }
                    cancel()
                }

                if (!isTerminated()) waitForTermination()
            }
        }

    } catch (exchangeException: IppExchangeException) {
        exchangeException.logDetails()
        log.error(exchangeException) { "inspect printer failed" }
    }

}