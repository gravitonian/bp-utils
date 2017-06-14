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
package org.acme.bestpublishing.services;

import org.acme.bestpublishing.constants.BestPubConstants;
import org.acme.bestpublishing.model.BestPubContentModel;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.acme.bestpublishing.constants.BestPubConstants.EPUB_OPEN_PUBLICATION_STRUCTURE_FOLDER_NAME;
import static org.acme.bestpublishing.constants.BestPubConstants.EPUB_PACKAGE_FILE_FILENAME;

/**
 * Service used to create the artifact that will be picked up by DARTS and then delivered to the Website.
 *
 * @author martin.bergljung@marversolutions.org
 * @version 1.0
 */
@Transactional(readOnly = true)
public class PublishingServiceImpl implements PublishingService {
    private static Logger LOG = LoggerFactory.getLogger(PublishingServiceImpl.class);

    /**
     * Alfresco Services
     */
    private ServiceRegistry serviceRegistry;
 
    /**
     * Best Publishing Services
     */
    private AlfrescoRepoUtilsService alfrescoRepoUtilsService;

    /**
     * Directory where web server delivery system will look for ISBN EPub files to publish
     */
    private String epubPickupDirectory;

    /**
     * Spring DI
     */

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public void setAlfrescoRepoUtilsService(AlfrescoRepoUtilsService alfrescoRepoUtilsService) {
        this.alfrescoRepoUtilsService = alfrescoRepoUtilsService;
    }

    public void setEpubPickupDirectory(String epubPickupDirectory) {
        this.epubPickupDirectory = epubPickupDirectory;
    }

    /**
     * Interface implementation
     */

    @Override
    @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public boolean createAndStoreEPubArtifact(NodeRef isbnFolderNodeRef) {
        String isbn = (String) serviceRegistry.getNodeService().getProperty(isbnFolderNodeRef, ContentModel.PROP_NAME);

        LOG.debug("Creating and saving the [{}.epub] file to [{}]...", isbn, epubPickupDirectory);

        boolean successfulOperation = false;
        File tempEPub = null;
        ZipOutputStream zo = null;
        boolean writeSuccessFull = false;

        try {
            // Create a temp file output stream
            tempEPub = File.createTempFile(UUID.randomUUID() + "", ".part");

            // Start a new output stream for the EPub file we want to create
            zo = new ZipOutputStream(new FileOutputStream(tempEPub));

            // Add the mimetype file to the top of the EPub
            addMimetypeFile2EPub(zo, isbn);

            // Add META_INF folder to EPub, including container.xml
            addMetaInfFolder2EPub(zo, isbn);

            // Add package.opf file to EPub
            addPackageFile2EPub(zo, isbn, isbnFolderNodeRef);

            // Add everything in the subfolders of Sites/book-management/documentLibrary/{year}/{isbn}
            addIsbnSubFolders2EPub(zo, isbn, isbnFolderNodeRef);

            // Take the published version from publishing version aspect and
            // calculate the new published version
            String newPublishedVersion = getNextPublishedVersion(isbnFolderNodeRef);

            // Set new version and publishing date against the ISBN/Book folder
            serviceRegistry.getNodeService().setProperty(isbnFolderNodeRef,
                    BestPubContentModel.WebPublishingInfoAspect.Prop.WEB_PUBLISHED_DATE, new Date());
            serviceRegistry.getNodeService().setProperty(isbnFolderNodeRef,
                    BestPubContentModel.WebPublishingInfoAspect.Prop.WEB_PUBLISHED_VERSION, newPublishedVersion);

            writeSuccessFull = true;
        } catch (IOException ioe) {
            LOG.error("Could not save the EPub artifact ZIP [{}.epub] to local directory [{}]: {}",
                    new Object[]{isbn, tempEPub, ioe});
        } finally {
            // Flush and close the ZIP output stream
            // so the the ZIP is ready to be moved
            try {
                if (zo != null) {
                    zo.flush();
                    zo.close();
                }
            } catch (IOException e) {
                LOG.error("Could not close the EPub artifact ZIP output stream for [{}]", isbn, e);
                writeSuccessFull = false;
            }

            if (writeSuccessFull) {
                // Make sure the EPub ZIP is moved with a temp name, and then renamed
                // This is to protect against situations when it is moved over filesystem boundaries
                // and the move is not atomic, the rename is always atomic on the same filesystem
                try {
                    if (epubPickupDirectory.lastIndexOf("/") != (epubPickupDirectory.length() - 1)) {
                        epubPickupDirectory = epubPickupDirectory + "/";
                    }

                    String epubArtifactName = isbn + ".epub";
                    File finalEPub = new File(epubPickupDirectory + epubArtifactName);
                    if (finalEPub.exists()) {
                        LOG.warn("Final EPub artifact ZIP already exists, and will be overwritten", finalEPub);
                    }

                    File epubPickupDirectory = new File(this.epubPickupDirectory);
                    java.nio.file.Path epubPickupDirPath = epubPickupDirectory.toPath();
                    java.nio.file.Path tempEPubDirPath =
                            Files.move(tempEPub.toPath(), epubPickupDirPath.resolve(tempEPub.getName()),
                                    StandardCopyOption.REPLACE_EXISTING);
                    
                    // On Windows you can't just rename a file if a file already exists
                    // in the folder with the filename
                    if (finalEPub.exists()) {
                    	finalEPub.delete();
                    }
                    
                    boolean renameSuccessfull = tempEPubDirPath.toFile().renameTo(finalEPub);
                    if (renameSuccessfull) {
                        LOG.debug("Book EPub artifact [{}] published with success!", isbn);
                        successfulOperation = true;
                    } else {
                        LOG.error("Could not rename EPub artifact [temp={}][final={}][exists={}][writable={}]",
                                new Object[]{tempEPub, finalEPub, finalEPub.exists(), finalEPub.canWrite()});
                    }
                } catch (IOException e) {
                    LOG.error("Could not move EPub artifact ZIP from tmp to /delivery for [{}]", isbn, e);
                }
            } else {
                LOG.error("Could not create EPub artifact {} for [{}]", tempEPub, isbn);
            }
        }

        if (successfulOperation) {
            LOG.debug("Finished creating and saving the [{}.epub] file to [{}]", isbn, epubPickupDirectory);
        }

        return successfulOperation;
    }

    /**
     * Creates the 'mimetype' file at the top of the EPub.
     *
     * @param zo ZIP Output stream to write Zip entries to and file content
     * @param isbn the ISBN number
     */
    private void addMimetypeFile2EPub(ZipOutputStream zo, String isbn) throws IOException {
        LOG.debug("Adding [{}] mimetype file to EPub ...", isbn);

        String parentFolder = "";
        String filename = "mimetype";
        String mimetype = "application/epub+zip";

        addFile2Zip(zo, filename, mimetype.getBytes(), parentFolder);

        LOG.debug("Finished adding [{}] mimetype file to EPub", isbn);
    }

    /**
     * Creates and populates the main META-INF folder at the top of the EPub file.
     * <br />
     * The structure looks like this:
     * /
     * __/META-INF
     * ____container.xml
     *
     * @param zo                ZIP Output stream to write Zip entries to and file content
     * @param isbn              the ISBN number
     */
    private void addMetaInfFolder2EPub(ZipOutputStream zo, String isbn)
            throws IOException {
        LOG.debug("Adding [{}] META-INF folder with container.xml ...", isbn);

        String folderName = "META-INF";
        String fileName = "container.xml";

        String containerXmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n" +
                "   <rootfiles>\n" +
                "      <rootfile full-path=\"OPS/package.opf\" media-type=\"application/oebps-package+xml\"/>\n" +
                "   </rootfiles>\n" +
                "</container>";

        addFile2Zip(zo, fileName, containerXmlContent.getBytes(), folderName);

        LOG.debug("Finished adding [{}] META-INF folder with container.xml", isbn);
    }

    /**
     * Adds the package.opf to the EPub.
     * The structure looks like this:
     * /
     * __/OPS
     * ____package.opf
     *
     * @param zo                ZIP Output stream to write Zip entries to and file content
     * @param isbn              the ISBN number
     * @param isbnFolderNodeRef the ISBN folder noderef for the book that should be published as EPub
     *                          (i.e. /Company Home/Sites/book-management/documentLibrary/{year}/{isbn})
     */
    private void addPackageFile2EPub(ZipOutputStream zo, String isbn, NodeRef isbnFolderNodeRef) throws IOException {
        NodeRef epubPackageFileNodeRef = alfrescoRepoUtilsService.getChildByName(
                isbnFolderNodeRef, EPUB_PACKAGE_FILE_FILENAME);
        if (epubPackageFileNodeRef != null) {
            addContentNode2Zip(zo, epubPackageFileNodeRef, EPUB_OPEN_PUBLICATION_STRUCTURE_FOLDER_NAME);

            LOG.debug("Added [{}] to EPub {}", EPUB_PACKAGE_FILE_FILENAME, isbn);
        } else {
            throw new AlfrescoRuntimeException("EPub " + EPUB_PACKAGE_FILE_FILENAME +
                    " file with book layout is missing in folder [{}], cannot create EPub file");
        }
    }

    /**
     * Adds all the chapter content, artwork, and styles to the EPub.
     * <br />
     * The structure looks like this: <br />
     * /OPS
     * __/css
     * _____*.css
     * __/images
     * _____*.jpg
     * _____*.png
     * __/cover.xhtml
     * __/TOC.xhtml
     * __/s001-BookTitlePage-01.xhtml
     * __/s002-Copyright-01.xhtml
     * __/s003-Introduction-01.xhtml
     * __/s004-Chapter-001.xhtml
     * __/s005-Chapter-002.xhtml
     * ...
     *
     * @param zo                ZIP Output stream to write Zip entries to and file content
     * @param isbn              the ISBN number
     * @param isbnFolderNodeRef the ISBN folder that should be published   (i.e. /Company Home/RHO/{ISBN})
     */
    private void addIsbnSubFolders2EPub(ZipOutputStream zo, String isbn, NodeRef isbnFolderNodeRef)
            throws IOException {
        LOG.debug("Adding [{}] ISBN folder's subfolders with chpater content, artworks, supplementary, and styles ...",
                isbn);

        // Path in EPub where to add main OPS folder content
        String opsFolderPathInEPub = EPUB_OPEN_PUBLICATION_STRUCTURE_FOLDER_NAME;

        // Add /Company Home/Sites/book-management/documentLibrary/{year}/{isbn}/Styles to OPS/css
        String stylesheetFolderPathInEPub = opsFolderPathInEPub + "/" + BestPubConstants.EPUB_STYLESHEET_FOLDER_NAME;
        NodeRef stylesFolderNodeRef = alfrescoRepoUtilsService.getChildByName(
                isbnFolderNodeRef, BestPubConstants.STYLES_FOLDER_NAME);
        if (stylesFolderNodeRef != null) {
            addFolderNodeContents2Zip(zo, stylesFolderNodeRef, stylesheetFolderPathInEPub);
            LOG.debug("{} created for {}.", stylesheetFolderPathInEPub, isbn);
        } else {
            LOG.debug("Skipping OPS/css in {} EPub as it is missing", isbn);
        }

        // Add /Company Home/Sites/book-management/documentLibrary/{year}/{isbn}/Artwork to OPS/images
        String artworkFolderPathInEPub = opsFolderPathInEPub + "/" + BestPubConstants.EPUB_IMAGES_FOLDER_NAME;
        NodeRef artworkFolderNodeRef = alfrescoRepoUtilsService.getChildByName(
                isbnFolderNodeRef, BestPubConstants.ARTWORK_FOLDER_NAME);
        if (artworkFolderNodeRef != null) {
            addFolderNodeContents2Zip(zo, artworkFolderNodeRef, artworkFolderPathInEPub);
            LOG.debug("{} created for {}.", artworkFolderPathInEPub, isbn);
        } else {
            LOG.debug("Skipping OPS/images in {} EPub as it is missing", isbn);
        }

        // Add /Company Home/Sites/book-management/documentLibrary/{year}/{isbn}/Supplementary to OPS
        String supplPathInZip = opsFolderPathInEPub;
        NodeRef supplementaryFolderNodeRef = alfrescoRepoUtilsService.getChildByName(
                isbnFolderNodeRef, BestPubConstants.SUPPLEMENTARY_FOLDER_NAME);
        if (supplementaryFolderNodeRef != null) {
            addFolderNodeContents2Zip(zo, supplementaryFolderNodeRef, supplPathInZip);
            LOG.debug("Added supplementary files to /OPS for {}.", isbn);
        } else {
            LOG.warn("Skipping supplementary files {} in EPub as folder is missing", isbn);
        }

        // Add all chapter content such as
        // /Company Home/Sites/book-management/documentLibrary/{year}/{isbn}/chapter-1 to OPS
        List<ChildAssociationRef> isbnFolderChildren = serviceRegistry.getNodeService().getChildAssocs(isbnFolderNodeRef);
        for (ChildAssociationRef childFolder : isbnFolderChildren) {
            NodeRef childFolderNodeRef = childFolder.getChildRef();
            if (serviceRegistry.getNodeService().getType(childFolderNodeRef).equals(
                    BestPubContentModel.ChapterFolderType.QNAME)) {
                addFolderNodeContents2Zip(zo, childFolderNodeRef, opsFolderPathInEPub);
            }
        }
        LOG.debug("Added chapter files to /OPS for {}.", isbn);

        LOG.debug("Finished adding [{}] ISBN folder's subfolders with chapter content, " +
                        "artworks, supplementary, and styles ...", isbn);
    }

    /**
     * Get the next published version number to set on ISBN/Book Folder.
     *
     * @param isbnFolderNodeRef the /Company Home/Sites/book-management/documentLibrary/{year}/{isbn} node ref
     * @return the version number to use when publishing the ISBN
     */
    private String getNextPublishedVersion(NodeRef isbnFolderNodeRef) {
        String nextVersion = "1.0";

        // Take the published version from publishing version aspect
        // calculate the new version of publishing
        if (serviceRegistry.getNodeService().hasAspect(
                isbnFolderNodeRef, BestPubContentModel.WebPublishingInfoAspect.QNAME)) {
            String currentVersion = (String) serviceRegistry.getNodeService().getProperty(isbnFolderNodeRef,
                    BestPubContentModel.WebPublishingInfoAspect.Prop.WEB_PUBLISHED_VERSION);
            if (StringUtils.isNotBlank(currentVersion)) {
                int majorVersion = Integer.parseInt(currentVersion.substring(0, currentVersion.indexOf(".")));
                nextVersion = (majorVersion + 1) + ".0";
            }
        }

        return nextVersion;
    }

    /**
     * Add a file, with content in memory, to the ZIP
     *
     * @param zo ZIP Output stream to write Zip entries to and file content
     * @param filename the name of the file that should be added to the ZIP
     * @param fileContent the content of the file that should be added to the ZIP
     * @param parentFolder this node should go in under in the ZIP
     */
    private void addFile2Zip(ZipOutputStream zo, String filename, byte[] fileContent,
                             String parentFolder) throws IOException {
        // Create a new ZIP Entry for the file
        zo.putNextEntry(new ZipEntry(createZipEntryPath(parentFolder, filename, false)));

        // Write file content to the ZIP
        InputStream fileIs = new BufferedInputStream(new ByteArrayInputStream(fileContent));

        byte[] buffer = new byte[4096];
        int bytesRead;

        // Read from is to buffer
        while ((bytesRead = fileIs.read(buffer)) > 0) {
            zo.write(buffer, 0, bytesRead);
        }

        // Close the content file input stream (no effect on bytearrayinputstream but good practice)
        fileIs.close();

        // Close ZIP entry and prepare to write another one
        zo.closeEntry();
    }

    /**
     * Add the passed in content node to the ZIP
     *
     * @param zo ZIP Output stream to write Zip entries to and file content
     * @param contentNodeRef the content node reference
     * @param parentFolderPath the content file should go in under this folder path in the ZIP
     * @throws IOException if could not add entry
     */
    private void addContentNode2Zip(ZipOutputStream zo, NodeRef contentNodeRef,
                                    String parentFolderPath)
            throws IOException {
        if (serviceRegistry.getDictionaryService().isSubClass(
                serviceRegistry.getNodeService().getType(contentNodeRef), ContentModel.TYPE_CONTENT)) {
            String filename = (String)serviceRegistry.getNodeService().getProperty(
                    contentNodeRef, ContentModel.PROP_NAME);

            // Create a new ZIP Entry for the file
            zo.putNextEntry(new ZipEntry(createZipEntryPath(parentFolderPath, filename, false)));

            // Write file content to the ZIP
            ContentReader fileContentReader = serviceRegistry.getFileFolderService().getReader(contentNodeRef);
            InputStream fileIs = new BufferedInputStream(fileContentReader.getContentInputStream());

            byte[] buffer = new byte[4096];
            int bytesRead;

            // Read from is to buffer
            while ((bytesRead = fileIs.read(buffer)) > 0) {
                zo.write(buffer, 0, bytesRead);
            }

            // Close the content file input stream
            fileIs.close();

            // Close ZIP entry and prepare to write another one
            zo.closeEntry();
        } else {
            throw new IllegalArgumentException("This method should be called with a content node reference");
        }
    }

    /**
     * Add all the child nodes for passed on folder node to the ZIP
     *
     * @param zo ZIP Output stream to write Zip entries to and file content
     * @param folderNodeRef the folder node reference
     * @param parentFolderPath all child nodes should go in under this folder path in the ZIP
     * @throws IOException if could not add entry
     */
    private void addFolderNodeContents2Zip(ZipOutputStream zo, NodeRef folderNodeRef,
                                           String parentFolderPath)
            throws IOException {
        if (serviceRegistry.getDictionaryService().isSubClass(serviceRegistry.getNodeService().getType(folderNodeRef), ContentModel.TYPE_FOLDER)) {
            // Read all the nodes that should go into the DARTS artifact and write to ZIP
            List<ChildAssociationRef> folderChildNodes = serviceRegistry.getNodeService().getChildAssocs(folderNodeRef);
            for (ChildAssociationRef folderChildNode : folderChildNodes) {
                NodeRef fileNodeRef = folderChildNode.getChildRef();
                String filename = (String)serviceRegistry.getNodeService().getProperty(fileNodeRef, ContentModel.PROP_NAME);

                // Create a new ZIP Entry for the file
                zo.putNextEntry(new ZipEntry(createZipEntryPath(parentFolderPath, filename, false)));

                // Write file content to the ZIP
                ContentReader fileContentReader = serviceRegistry.getFileFolderService().getReader(fileNodeRef);
                InputStream fileIs = new BufferedInputStream(fileContentReader.getContentInputStream());

                byte[] buffer = new byte[4096];
                int bytesRead;

                // Read from is to buffer
                while ((bytesRead = fileIs.read(buffer)) > 0) {
                    zo.write(buffer, 0, bytesRead);
                }

                // Close the content file input stream
                fileIs.close();

                // Close ZIP entry and prepare to write another one
                zo.closeEntry();
            }
        } else {
            throw new IllegalArgumentException("This method should be called with a folder node reference");
        }
    }

    /**
     * Create a ZIP entry path based on passed in parent path and file or folder name.
     *
     * @param parentPath the ZIP directory path to the file or folder
     * @param name the filename or folder name
     * @param isFolder true if the ZIP entry should represent a folder, false if file
     * @return the path to the ZIP entry
     */
    private String createZipEntryPath(String parentPath, String name, boolean isFolder) {
        String pathInZip = name;
        if (StringUtils.isNotBlank(parentPath)) {
            pathInZip = parentPath + "/" + name;
        }
        if (isFolder) {
            pathInZip = pathInZip + "/"; // Folder entry has to end in /
        }

        return pathInZip;
    }
}
