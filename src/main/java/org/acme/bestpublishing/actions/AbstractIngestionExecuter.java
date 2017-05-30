/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.acme.bestpublishing.actions;

import org.acme.bestpublishing.error.ProcessingErrorCode;
import org.acme.bestpublishing.exceptions.IngestionException;
import org.acme.bestpublishing.services.AlfrescoRepoUtilsService;
import org.acme.bestpublishing.services.BestPubUtilsService;
import org.acme.bestpublishing.services.IngestionService;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.springframework.jmx.export.annotation.ManagedAttribute;

import java.io.File;
import java.util.Date;

//import org.springframework.jmx.export.annotation.ManagedMetric;
//import org.springframework.jmx.support.MetricType;

/**
 * Abstract ingestion executer that has functionality to check for zip files in a specified folder.
 *
 * @author martin.bergljung@marversolutions.org
 * @version 1.0
 */
public abstract class AbstractIngestionExecuter {

    /**
     * Alfresco Services
     */
    protected ServiceRegistry serviceRegistry;

    /**
     * Best Pub Specific services
     */
    protected BestPubUtilsService bestPubUtilsService;
    protected AlfrescoRepoUtilsService alfrescoRepoUtilsService;
    protected IngestionService ingestionService;

    /**
     * Content Ingestion config
     */
    private String filesystemPathToCheck;
    private String alfrescoFolderPath;
    private String cronExpression;
    private int cronStartDelay;

    /**
     * Content Ingestion stats
     */
    private Date lastRunTime;
    private long numberOfRuns;
    private int zipQueueSize;

    /**
     * Spring Dependency Injection
     */
    public void setFilesystemPathToCheck(String filesystemPathToCheck) {
        this.filesystemPathToCheck = filesystemPathToCheck;
    }

    public void setAlfrescoFolderPath(String alfrescoFolderPath) {
        this.alfrescoFolderPath = alfrescoFolderPath;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public void setCronStartDelay(int cronStartDelay) {
        this.cronStartDelay = cronStartDelay;
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public void setAlfrescoRepoUtilsService(AlfrescoRepoUtilsService alfrescoRepoUtilsService) {
        this.alfrescoRepoUtilsService = alfrescoRepoUtilsService;
    }

    public void setIngestionService(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    public void setBestPubUtilsService(BestPubUtilsService bestPubUtilsService) {
        this.bestPubUtilsService = bestPubUtilsService;
    }

    /**
     * Managed Properties (JMX)
     */
    @ManagedAttribute(description = "Path to ZIP files")
    public String getFilesystemPathToCheck() {
        return this.filesystemPathToCheck;
    }

    @ManagedAttribute(description = "Cron expression controlling execution")
    public String getCronExpression() {
        return this.cronExpression;
    }

    @ManagedAttribute(description = "Ingestion start delay after bootstrap (ms)")
    public int getCronStartDelay() {
        return this.cronStartDelay;
    }

    @ManagedAttribute(description = "Last time it was called")
    public Date getLastRunTime() {
        return this.lastRunTime;
    }

    @ManagedAttribute(description = "Number of times it has run")
    public long getNumberOfRuns() {
        return this.numberOfRuns;
    }

    //    @ManagedMetric(category="utilization", displayName="ZIP Queue Size",
    //          description="The size of the ZIP File Queue",
    //        metricType = MetricType.COUNTER, unit="zips")
    public long getZipQueueSize() {
        return this.zipQueueSize;
    }

    /**
     * To be implemented by sub classes
     */
    public abstract Logger getLog();
    public abstract boolean processZipFile(File zipFile, String extractedISBN, NodeRef alfrescoUploadFolderNodeRef);

    /**
     * Executer implementation
     */
    public void execute(String ingestionType) {
        getLog().debug("Checking for " + ingestionType + " ZIPs...");

        // Running stats
        lastRunTime = new Date();
        numberOfRuns++;

        // Get the node references for the /Company Home/Data Dictionary/BestPub/Incoming/[Content|Metadata]
        // folder where this ingestion action will upload the content
        NodeRef incomingAlfrescoFolderNodeRef = alfrescoRepoUtilsService.getNodeByXPath(alfrescoFolderPath);

        try {
            getLog().debug("File path to check = [{}]", filesystemPathToCheck);

            File folder = new File(filesystemPathToCheck);
            if (!folder.exists()) {
                throw new IngestionException(ProcessingErrorCode.INGESTION_DIR_NOT_FOUND);
            }
            if (!folder.isDirectory()) {
                throw new IngestionException(ProcessingErrorCode.INGESTION_DIR_IS_FILE);
            }

            File[] zipFiles = bestPubUtilsService.findFilesUsingExtension(folder, "zip");
            zipQueueSize = zipFiles.length;
            getLog().debug("Found [{}] " + ingestionType + " files", zipFiles.length);

            for (File zipFile : zipFiles) {
                String isbn = FilenameUtils.removeExtension(zipFile.getName());
                if (!bestPubUtilsService.isISBN(isbn)) {
                    getLog().error("Error processing " + ingestionType +
                            " zip file [{}], filename is not an ISBN number", zipFile.getName());

                    throw new IngestionException(ProcessingErrorCode.INGESTION_NO_ISBN_IN_ZIP_NAME);
                }

                if (processZipFile(zipFile, isbn, incomingAlfrescoFolderNodeRef)) {
                    // All done, delete the ZIP
                    zipFile.delete();
                } else {
                    // Something went wrong when processing the zip file,
                    // move it to a directory for ZIPs that failed processing
                    bestPubUtilsService.moveZipToDirForFailedProcessing(zipFile, filesystemPathToCheck);
                }

                zipQueueSize--;
            }

            getLog().debug("Processed [{}] " + ingestionType + " ZIP files", zipFiles.length);
        } catch (Exception e) {
            getLog().error("Encountered an error when ingesting " + ingestionType + " - exiting", e);
        }
    }
 }
