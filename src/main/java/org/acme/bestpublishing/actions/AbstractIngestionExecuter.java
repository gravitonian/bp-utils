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

import org.acme.bestpublishing.exceptions.IngestionException;
import org.acme.bestpublishing.model.BestPubContentModel;
import org.acme.bestpublishing.services.AlfrescoRepoUtilsService;
import org.acme.bestpublishing.services.BestPubUtilsService;
import org.acme.bestpublishing.services.IngestionService;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.springframework.jmx.export.annotation.ManagedAttribute;

import java.io.File;
import java.io.IOException;
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

    public abstract Logger getLog();

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
        NodeRef contentIncomingFolderNodeRef = alfrescoRepoUtilsService.getNodeByXPath(alfrescoFolderPath);

        try {
            getLog().debug("File path to check = [{}]", filesystemPathToCheck);

            File folder = new File(filesystemPathToCheck);
            if (!folder.exists()) {
                throw new IngestionException("Folder to check does not exist.");
            }
            if (!folder.isDirectory()) {
                throw new IngestionException("The file path must be to a directory.");
            }

            File[] zipFiles = bestPubUtilsService.findFilesUsingExtension(folder, "zip");
            zipQueueSize = zipFiles.length;
            getLog().debug("Found [{}] " + ingestionType + " files", zipFiles.length);

            for (File zipFile : zipFiles) {
                if (processZipFile(zipFile, contentIncomingFolderNodeRef)) {
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

    /**
     * Process one ZIP file and upload its content to Alfresco
     *
     * @param zipFile              the ZIP file that should be processed and uploaded
     * @param alfrescoUploadFolderNodeRef the target folder for new ISBN content or metadata
     * @return true if processed file ok, false if there was an error
     */
    private boolean processZipFile(File zipFile, NodeRef alfrescoUploadFolderNodeRef)
            throws IOException {
        getLog().debug("Processing zip file [{}]", zipFile.getName());

        String isbn = FilenameUtils.removeExtension(zipFile.getName());
        if (!bestPubUtilsService.isISBN(isbn)) {
            getLog().error("Error processing zip file [{}], filename is not an ISBN number", zipFile.getName());

            return false;
        }

        // Check if ISBN already exists under /Company Home/Data Dictionary/BestPub/Incoming/[Content|Metadata]
        NodeRef targetAlfrescoFolderNodeRef = null;
        NodeRef isbnFolderNodeRef = alfrescoRepoUtilsService.getChildByName(alfrescoUploadFolderNodeRef, isbn);
        if (isbnFolderNodeRef == null) {
            // We got a new ISBN that has not been published before
            // And this means uploading to /Data Dictionary/BestPub/Incoming/[Content|Metadata]
            targetAlfrescoFolderNodeRef = alfrescoUploadFolderNodeRef;

            getLog().debug("Found new ISBN {} that has not been published before, " +
                    "uploading to /Data Dictionary/BestPub/Incoming/[Content|Metadata]", isbn);
        } else {
            // We got an ISBN that has already been published, so we need to republish it

            // However, first verify that content has been previously completely ingested into the
            // /Data Dictionary/BestPub/Incoming/[Content|Metadata]/{ISBN} folder and has
            // property bestpub:ingestionStatus set to Complete.
            if (serviceRegistry.getNodeService().getProperty(isbnFolderNodeRef,
                    BestPubContentModel.BookFolderType.Prop.INGESTION_STATUS).
                    equals(BestPubContentModel.IngestionStatus.COMPLETE.toString())) {

                getLog().debug("Found updated ISBN {} that has been published before, " +
                        "uploading again to /Data Dictionary/BestPub/Incoming/[Content|Metadata]", isbn);

                // Delete old one if it exists (Content can be re-published multiple times)
                isbnFolderNodeRef = alfrescoRepoUtilsService.getChildByName(alfrescoUploadFolderNodeRef, isbn);
                if (isbnFolderNodeRef != null && serviceRegistry.getNodeService().exists(isbnFolderNodeRef)) {
                    serviceRegistry.getNodeService().deleteNode(isbnFolderNodeRef);
                }
            } else {
                // We got a new ISBN that has had interrupted ingestion, upload again to
                // /Data Dictionary/BestPub/Incoming/[Content|Metadata]
                targetAlfrescoFolderNodeRef = alfrescoUploadFolderNodeRef;

                // Delete the interrupted ingestion folder
                serviceRegistry.getNodeService().deleteNode(isbnFolderNodeRef);

                getLog().debug("Found new ISBN {} that has had interrupted ingestion, " +
                        "only ISBN folder exist with Ingestion Status = In Progress, " +
                        "uploading again to /Data Dictionary/BOPP/Incoming/[Content|Metadata]", isbn);
            }
        }

        try {
            ingestionService.importZipFileContent(zipFile, targetAlfrescoFolderNodeRef, isbn);
            return true;
        } catch (Exception e) {
            getLog().error("Error processing zip file " + zipFile.getName(), e);
        }

        return false;
    }
}
