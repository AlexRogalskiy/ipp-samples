package ipp;

import de.gmuth.ipp.client.CupsClient;

public class PauseCupsPrinter {

  public static void main(String[] args) {
    CupsClient cupsClient = new CupsClient("localhost");
    cupsClient.basicAuth("admin", "<secret>");
    cupsClient.getPrinter("ColorJet_HP").pause();
  }

}