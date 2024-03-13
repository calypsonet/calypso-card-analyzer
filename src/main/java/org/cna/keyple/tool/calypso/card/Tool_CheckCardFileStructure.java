/* **************************************************************************************
 * Copyright (c) 2019 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.cna.keyple.tool.calypso.card;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.cna.keyple.tool.calypso.carddata.*;
import org.cna.keyple.tool.calypso.carddata.AccessConditions.AccessCondition;
import org.cna.keyple.tool.calypso.common.ToolUtils;
import org.eclipse.keyple.core.util.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tool_CheckCardFileStructure {

  private static final Logger logger = LoggerFactory.getLogger(Tool_CheckCardFileStructure.class);

  private static List<FileReference> dataRefStatus;

  private static void checkAccessCondition(
      int groupNumber, AccessCondition dataToCheck, AccessCondition dataRead) {

    if (dataToCheck.getAccessCondition() != null
        && !dataToCheck.getAccessCondition().equals(dataRead.getAccessCondition())) {
      logger.info(
          "Group "
              + groupNumber
              + ":: Expected Access Condition to be ["
              + dataToCheck.getAccessCondition()
              + "] and found ["
              + dataRead.getAccessCondition()
              + "].");
    }

    if (dataToCheck.getAccessCondition().equals("10")
        || dataToCheck.getAccessCondition().equals("14")
        || dataToCheck.getAccessCondition().equals("15")) {

      if (dataToCheck.getKeyLevel() == null) {
        logger.info(
            "Group "
                + groupNumber
                + ":: Structure to check error. Access Condition ["
                + dataToCheck.getAccessCondition()
                + "] requires an associated Key Level.");
      } else if (!dataToCheck.getKeyLevel().equals(dataRead.getKeyLevel())) {
        logger.info(
            "Group "
                + groupNumber
                + ":: Expected Key Level to be ["
                + dataToCheck.getKeyLevel()
                + "] and found ["
                + dataRead.getKeyLevel()
                + "].");
      }
    }
  }

  private static void checkAccessConditions(
      AccessConditions dataToCheck, AccessConditions dataRead) {

    checkAccessCondition(0, dataToCheck.getGroup0(), dataRead.getGroup0());
    checkAccessCondition(1, dataToCheck.getGroup1(), dataRead.getGroup1());
    checkAccessCondition(2, dataToCheck.getGroup2(), dataRead.getGroup2());
    checkAccessCondition(3, dataToCheck.getGroup3(), dataRead.getGroup3());
  }

  private static void checkString(String name, String dataToCheck, String dataRead) {

    if (dataToCheck != null && !dataToCheck.equals(dataRead)) {
      logger.info(
          "Expected " + name + " to be [" + dataToCheck + "] and found [" + dataRead + "].");
    }
  }

  private static void checkDataRef(FileReference refToCheck) {

    if (dataRefStatus.size() > 0) {

      Iterator dataRefIterator = dataRefStatus.iterator();

      while (dataRefIterator.hasNext()) {

        FileReference fileRef = (FileReference) dataRefIterator.next();

        if (fileRef.getLinkedFileLid().equals(refToCheck.getBaseFileLid())) {

          if (fileRef.getRefValueRead().equals(refToCheck.getRefValueRead())) {
            fileRef.setReferenceFoundFlag(true);
          } else {
            logger.info(
                "DataRef for file "
                    + refToCheck.getBaseFileLid()
                    + " ["
                    + refToCheck.getRefValueRead()
                    + "] doesn't match the value of the linked file "
                    + fileRef.getBaseFileLid()
                    + " ["
                    + fileRef.getRefValueRead()
                    + "].");
          }

          return;
        }
      }
    }

    dataRefStatus.add(refToCheck);
  }

  private static void checkCardFileData(CardFileData dataToCheck, List<CardFileData> fileListRead) {

    if (dataToCheck.getSfi() == null) {
      logger.info("Invalid structure to check. File doesn't have an SFI defined.");
      return;
    }

    Iterator fileListReadIterator = fileListRead.iterator();

    while (fileListReadIterator.hasNext()) {

      CardFileData dataRead = (CardFileData) fileListReadIterator.next();

      if (dataToCheck.getSfi().equals(dataRead.getSfi())) {

        logger.info("Checking file with SFI (" + dataToCheck.getSfi() + ")");

        checkString("LID", dataToCheck.getLid(), dataRead.getLid());

        checkString("EF Type", dataToCheck.getEfType(), dataRead.getEfType());

        checkString("Record Size", dataToCheck.getRecSize(), dataRead.getRecSize());

        checkString("Number of Records", dataToCheck.getNumRec(), dataRead.getNumRec());

        checkAccessConditions(dataToCheck.getAccessConditions(), dataRead.getAccessConditions());

        // Check simulated counter data ref?
        if (dataToCheck.getDataRef() != null) {

          if (dataRead.getDataRef() == null) {
            logger.info("DataRef field not present in the card!");
          } else {
            FileReference fileRef =
                new FileReference(
                    dataToCheck.getLid(), dataToCheck.getDataRef(), dataRead.getDataRef());
            checkDataRef(fileRef);
          }
        }
        return;
      }
    }

    logger.info("No matching file found for SFI (" + dataToCheck.getSfi() + ")");
  }

  private static void checkApplicationType(String dataToCheck, String dataRead) {

    if (dataToCheck == null || dataRead == null) {
      return;
    }

    int applicationTypeToCheck = Integer.parseInt(dataToCheck, 16);

    int applicationTypeRead = Integer.parseInt(dataRead, 16);

    if (applicationTypeToCheck >= 0x06 && applicationTypeToCheck <= 0x1F) {
      checkString("Application Type", dataToCheck, dataRead);
      return;
    }

    if ((applicationTypeToCheck & 0x01) != (applicationTypeRead & 0x01)) {
      logger.info(
          "Incorrect value for PIN configuration flag. Expected ("
              + (applicationTypeToCheck & 0x01)
              + ") and got ("
              + (applicationTypeRead & 0x01)
              + ").");
    }

    if ((applicationTypeToCheck & 0x02) != (applicationTypeRead & 0x02)) {
      logger.info(
          "Incorrect value for SV configuration flag. Expected ("
              + (applicationTypeToCheck & 0x02)
              + ") and got ("
              + (applicationTypeRead & 0x02)
              + ").");
    }

    if ((applicationTypeToCheck & 0x08) != (applicationTypeRead & 0x08)) {
      logger.info(
          "Incorrect value for Rev 3.2 mode support configuration flag. Expected ("
              + (applicationTypeToCheck & 0x08)
              + ") and got ("
              + (applicationTypeRead & 0x08)
              + ").");
    }

    if ((applicationTypeToCheck & 0x10) != (applicationTypeRead & 0x10)) {
      logger.info(
          "Incorrect value for Rev 3.3 PKI mode support configuration flag. Expected ("
              + (applicationTypeToCheck & 0x10)
              + ") and got ("
              + (applicationTypeRead & 0x10)
              + ").");
    }
  }

  private static void checkCardAppData(
      CardApplicationData dataToCheck, CardApplicationData dataRead) {

    checkString(
        "Calypso Revision", dataToCheck.getCalypsoRevision(), dataRead.getCalypsoRevision());

    checkString("Session Modifications", dataToCheck.getSessionModif(), dataRead.getSessionModif());

    checkApplicationType(dataToCheck.getApplicationType(), dataRead.getApplicationType());

    checkString(
        "Application Subtype",
        dataToCheck.getApplicationSubtype(),
        dataRead.getApplicationSubtype());

    checkAccessConditions(dataToCheck.getAccessConditions(), dataRead.getAccessConditions());

    checkString("KIF1", dataToCheck.getKif1(), dataRead.getKif1());
    checkString("KIF2", dataToCheck.getKif2(), dataRead.getKif2());
    checkString("KIF3", dataToCheck.getKif3(), dataRead.getKif3());
    checkString("KVC1", dataToCheck.getKvc1(), dataRead.getKvc1());
    checkString("KVC2", dataToCheck.getKvc2(), dataRead.getKvc2());
    checkString("KVC3", dataToCheck.getKvc3(), dataRead.getKvc3());

    checkString("LID", dataToCheck.getLid(), dataRead.getLid());

    if (dataToCheck.getFileList().size() != dataRead.getFileList().size()) {
      logger.info(
          "Expected application to have ("
              + dataToCheck.getFileList().size()
              + ") file(s) and found ("
              + dataRead.getFileList().size()
              + ").");
    }

    Iterator fileListToCheckIter = dataToCheck.getFileList().iterator();

    while (fileListToCheckIter.hasNext()) {
      checkCardFileData((CardFileData) fileListToCheckIter.next(), dataRead.getFileList());
    }
  }

  public static void main(String[] args) {

    boolean isCardPresent = Tool_AnalyzeCardFileStructure.initReaders();

    CardStructureData fileStructureToCheck;

    dataRefStatus = new ArrayList<FileReference>();

    Gson gson =
        new GsonBuilder()
            .registerTypeHierarchyAdapter(byte[].class, new ToolUtils.HexTypeAdapter())
            .setPrettyPrinting()
            .create();

    try {
      fileStructureToCheck =
          gson.fromJson(
              new FileReader("TestKit_CalypsoPrimeRegularProfile_v3.json"),
              CardStructureData.class);

    } catch (Exception e) {
      logger.error("Exception while loading file structure to check " + e.getCause());
      return;
    }

    /* Check if a card is present in the reader */
    if (isCardPresent) {

      Iterator appToCheckListIter = fileStructureToCheck.getApplicationList().iterator();

      while (appToCheckListIter.hasNext()) {

        List<CardApplicationData> cardAppDataList = new ArrayList<CardApplicationData>();
        CardApplicationData cardAppDataToCheck = (CardApplicationData) appToCheckListIter.next();

        logger.info(
            "========================================================================================================");
        logger.info("Checking Application:: " + HexUtil.toHex(cardAppDataToCheck.getAid()));

        Tool_AnalyzeCardFileStructure.getApplicationsData(
            HexUtil.toHex(cardAppDataToCheck.getAid()), cardAppDataList);

        if (cardAppDataList.size() == 0) {
          logger.info("Application not present in card!");
        } else if (cardAppDataList.size() > 1) {
          logger.info(
              "Found ("
                  + cardAppDataList.size()
                  + ") applications for the given AID. Aborting application analysis");
        } else {
          checkCardAppData(cardAppDataToCheck, cardAppDataList.get(0));
        }
      }

      logger.info(
          "========================================================================================================");

      Iterator dataRefIter = dataRefStatus.iterator();

      while (dataRefIter.hasNext()) {

        FileReference fileRef = (FileReference) dataRefIter.next();

        if (fileRef.getReferenceFoundFlag() != true) {
          logger.info(
              "No/Incorrect data ref found for file "
                  + fileRef.getBaseFileLid()
                  + ". Should be linked with file "
                  + fileRef.getLinkedFileLid());
        }
      }

      logger.info(
          "========================================================================================================");
    }
  }
}
