package ipp

import de.gmuth.ipp.client.IppColorMode
import de.gmuth.ipp.client.IppPrinter
import de.gmuth.ipp.client.IppTemplateAttributes.documentFormat
import de.gmuth.ipp.client.IppTemplateAttributes.jobName
import de.gmuth.ipp.client.IppTemplateAttributes.pageRanges
import ipp.Printers.laser
import java.io.File

fun main() {
    //JulAdapter.configure("/ipp-client-logging.conf")
    val ippPrinter = IppPrinter(laser).apply {
        logDetails()
    }

    val file = File("test-docs/A4-ten-pages.pdf")
    ippPrinter.printJob(
            file,
            documentFormat("application/pdf"),
            jobName(file.name),
            pageRanges(2..5),
            IppColorMode.Monochrome
    ).apply {
        waitForTermination()
        logDetails()
    }
}