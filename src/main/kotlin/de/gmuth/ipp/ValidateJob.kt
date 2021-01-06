package de.gmuth.ipp

import de.gmuth.ipp.client.IppColorMode
import de.gmuth.ipp.client.IppMedia
import de.gmuth.ipp.client.IppPrinter
import de.gmuth.ipp.client.IppSides
import de.gmuth.ipp.client.IppTemplateAttributes.documentFormat
import de.gmuth.ipp.client.IppTemplateAttributes.printerResolution
import de.gmuth.ipp.core.IppExchangeException
import de.gmuth.ipp.core.IppResolution.Unit.DPI

fun main() {
    val ippPrinter = IppPrinter("ipp://localhost:8632/printers/laser")

    val ippValidationResponse = try {
        ippPrinter.validateJob(
                documentFormat("application/pdf"),
                printerResolution(600, DPI),
                IppColorMode.Color,
                IppSides.TwoSidedLongEdge,
                IppMedia.Collection(source = "manual")
        )

    } catch (ippExchangeException: IppExchangeException) {
        println(ippExchangeException)
        ippExchangeException.ippResponse
    }

    with(ippValidationResponse) {
        println("status: $status")
        if (unsupportedGroup.size > 0) {
            println("unsupported attributes or values:")
            for (ippAttribute in unsupportedGroup.values) {
                println(ippAttribute)
            }
        }
    }

}