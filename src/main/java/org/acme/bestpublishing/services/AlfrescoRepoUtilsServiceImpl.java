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

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.Tika;
import org.acme.bestpublishing.constants.BestPubConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.WebScriptResponse;

import java.io.*;
import java.io.FileNotFoundException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Implementation of the Alfresco repository helper, which manages and searches for stuff in local Alfresco repository
 * <p/>
 * Note. these are non-transactional call requiring an existing transaction to be in place.
 * Note2. proper permission checks also need to be in place before making calls.
 *
 * @author martin.bergljung@marversoutions.org
 * @version 1.0
 */
public class AlfrescoRepoUtilsServiceImpl implements AlfrescoRepoUtilsService {
    private static final Logger LOG = LoggerFactory.getLogger(AlfrescoRepoUtilsServiceImpl.class);

    /**
     * Hash type to use when computing content hash that will be used to compare content files for equality
     */
    private static final String MD5_HASH_TYPE = "md5";

    /**
     * Buffer when reading content bytes
     */
    private static final int BUFFER_SIZE = 4096;

    /**
     * Empty JSON object
     */
    private static final String EMPTY_JSON = "{}";

    /**
     * Alfresco Services
     */
    private ServiceRegistry serviceRegistry;

    /**
     * Spring Setter Injection
     */
    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public NodeRef getCompanyHome() {
        return serviceRegistry.getNodeLocatorService().getNode("companyhome", null, null);
    }

    @Override
    public NodeRef getDataDictionary() {
        return serviceRegistry.getNodeService().getChildByName(getCompanyHome(), ContentModel.ASSOC_CONTAINS,
                BestPubConstants.DATA_DICTIONARY_NAME);
    }

    @Override
    public NodeRef getNodeByDisplayPath(final String path) {
        String nodePath = path;
        // Make sure path does not start with /
        if (path.startsWith("/")) {
            nodePath = path.substring(1);
        }

        // Get the path elements, such as [ "Data Dictionary", "Email Templates" ]
        List<String> pathElements = Arrays.asList(StringUtils.split(nodePath, '/'));

        // Use the file folder service to resolve node reference for path elements
        NodeRef companyHome = getCompanyHome();
        NodeRef nodeRef = null;
        try {
            nodeRef = serviceRegistry.getFileFolderService().resolveNamePath(companyHome, pathElements).getNodeRef();
        } catch (org.alfresco.service.cmr.model.FileNotFoundException e) {
            LOG.error("Could not get NodeRef for path: " + path, e);
        }

        return nodeRef;
    }

    @Override
    public NodeRef getChildByName(final NodeRef parent, final String name) {
        NodeRef nodeRef = serviceRegistry.getNodeService().getChildByName(parent, ContentModel.ASSOC_CONTAINS, name);
        return nodeRef;
    }

    @Override
    public NodeRef getOrCreateFolder(final NodeRef parent, final String name) {
        NodeRef folder = getChildByName(parent, name);
        if (folder == null) {
            folder = serviceRegistry.getFileFolderService().create(parent, name, ContentModel.TYPE_FOLDER).getNodeRef();
            LOG.debug("Created sub-folder [{}] for [{}]", name, parent);
        } else {
            LOG.debug("Returning existing sub-folder [{}] for [{}]", name, parent);
        }
        return folder;
    }

    @Override
    public NodeRef createFolder(final NodeRef parentNodeRef, final String name, final QName type) {
        NodeRef folderNodeRef = getChildByName(parentNodeRef, name);
        if (folderNodeRef == null) {
            QName associationType = ContentModel.ASSOC_CONTAINS;
            QName associationQName = QName.createQName(
                    NamespaceService.CONTENT_MODEL_1_0_URI, QName.createValidLocalName(name));
            QName nodeType = type;
            Map<QName, Serializable> nodeProperties = new HashMap<QName, Serializable>();
            nodeProperties.put(ContentModel.PROP_NAME, name);
            ChildAssociationRef childAssocRef = serviceRegistry.getNodeService().createNode(
                    parentNodeRef, associationType, associationQName, nodeType, nodeProperties);

            return childAssocRef.getChildRef();
        }

        return folderNodeRef;
    }

    @Override
    public NodeRef getOrCreateFile(final NodeRef parentFolderNodeRef, final String filename,
                                   final String mimeType, final String content) {
        NodeRef file = getChildByName(parentFolderNodeRef, filename);
        if (file == null) {
            file = createFile(parentFolderNodeRef, filename, mimeType, content);
            LOG.debug("Created file [{}] in [{}]", filename, parentFolderNodeRef);
        } else {
            LOG.debug("Returning existing file [{}] in [{}]", filename, parentFolderNodeRef);
        }

        return file;
    }

    @Override
    public NodeRef createFileMetadataOnly(final NodeRef parentFolderNodeRef, final String filename) {
        NodeRef fileNodeRef = getChildByName(parentFolderNodeRef, filename);
        if (fileNodeRef != null) {
            LOG.error("File [{}] already exists, cannot create", fileNodeRef);
            return null;
        }

        ChildAssociationRef parentChildAssocRef = null;
        QName associationType = ContentModel.ASSOC_CONTAINS;
        QName associationQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,
                QName.createValidLocalName(filename));
        QName nodeType = ContentModel.TYPE_CONTENT;
        Map<QName, Serializable> nodeProperties = new HashMap<QName, Serializable>();
        nodeProperties.put(ContentModel.PROP_NAME, filename);
        parentChildAssocRef = serviceRegistry.getNodeService().createNode(
                parentFolderNodeRef, associationType, associationQName, nodeType, nodeProperties);
        NodeRef newFileNodeRef = parentChildAssocRef.getChildRef();
        return newFileNodeRef;
    }

    @Override
    public NodeRef createFile(final NodeRef parentFolderNodeRef, final File file) {
        try {
            return createFile(parentFolderNodeRef, file.getName(), new BufferedInputStream(new FileInputStream(file)));
        } catch (FileNotFoundException e) {
            LOG.error("Could not create node in Alfresco for file {}, [error={}]", file.getName(), e.getMessage());
        }
        return null;
    }

    @Override
    public NodeRef createFile(final NodeRef parentFolderNodeRef, final String filename,
                              final InputStream fileInputStream) {
        LOG.debug("Creating node and writing content for file [{}]", filename);

        // Create file node metadata
        NodeRef newFileNodeRef = createFileMetadataOnly(parentFolderNodeRef, filename);
        if (newFileNodeRef == null) {
            LOG.error("Node metadata for file [{}] could not be created", filename);
            return null;
        }

        // Add content bytes to file node
        try {
            // Get the MIMEType for the file we are adding
            // Note. There is a bug in Files.probeContentType so it always returns plain/text on Ubuntu 11
            // String mimeType = Files.probeContentType(file.toPath());
            Tika tika = new Tika();
            String mimeType = tika.detect(fileInputStream, filename);
            LOG.debug("Mime type for [{}] is [{}]", filename, mimeType);

            // Set content bytes for the new file node
            boolean updateContentPropertyAutomatically = true;
            ContentWriter writer = serviceRegistry.getContentService().getWriter(
                    newFileNodeRef, ContentModel.PROP_CONTENT, updateContentPropertyAutomatically);
            writer.setMimetype(mimeType);
            writer.putContent(fileInputStream); // Closes streams
        } catch (IOException ioe) {
            LOG.error("Error determining mime type for file [" + filename + "]", ioe);
        }

        return newFileNodeRef;
    }

    @Override
    public NodeRef createFile(final NodeRef parentFolderNodeRef, final String filename,
                              final String mimeType, final String content) {
        // Create file node metadata
        NodeRef newFileNodeRef = createFileMetadataOnly(parentFolderNodeRef, filename);
        if (newFileNodeRef == null) {
            LOG.error("Node metadata for file [{}] could not be created", filename);
            return null;
        }

        // Add content to file node
        boolean updateContentPropertyAutomatically = true;
        ContentWriter writer = serviceRegistry.getContentService().getWriter(newFileNodeRef, ContentModel.PROP_CONTENT,
                updateContentPropertyAutomatically);
        writer.setMimetype(mimeType);
        writer.putContent(content);

        return newFileNodeRef;
    }

    @Override
    public NodeRef searchOne(final String query) {
        List<NodeRef> matchingNodes = search(query);

        if (matchingNodes.isEmpty()) {
            return null;
        }

        return matchingNodes.get(0);
    }

    @Override
    public List<NodeRef> search(final String query) {
        LOG.debug("Executing Lucene query [{}]", query);

        StoreRef workspaceStoreRef = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
        ResultSet results = null;
        List<NodeRef> matchingNodes = new ArrayList<>();

        try {
            results = serviceRegistry.getSearchService().query(workspaceStoreRef, SearchService.LANGUAGE_LUCENE, query);
        } finally {
            if (results != null) {
                LOG.debug("Found [{}] nodes that matches query [{}]", results.length(), query);
                matchingNodes = results.getNodeRefs();
                // Close underlying resources used by the search engine
                results.close();
            } else {
                LOG.debug("Found [0] nodes that matches query [{}]", query);
                matchingNodes = new ArrayList<>();
            }
        }

        return matchingNodes;
    }


    @Override
    public boolean isPartOfGroup(final String username, final String groupName) {
        final Set<String> authoritiesForCurrentUser =
                serviceRegistry.getAuthorityService().getAuthoritiesForUser(username);
        if (authoritiesForCurrentUser.contains(groupName)) {
            return true;
        }
        return false;
    }

    @Override
    public NodeRef getNodeByXPath(final String path) {
        StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");

        ResultSet rs = serviceRegistry.getSearchService().query(storeRef, SearchService.LANGUAGE_XPATH, path);
        NodeRef nodeRef = null;
        try {
            if (rs.length() == 1) {
                nodeRef = rs.getNodeRef(0);
            }
        } finally {
            rs.close();
        }
        return nodeRef;
    }

    @Override
    public NodeRef getOrCreateXMLFileMetadata(final NodeRef parentNodeRef, final String fileName) {
        NodeRef xmlFileNodeRef = serviceRegistry.getFileFolderService().searchSimple(parentNodeRef, fileName);
        if (xmlFileNodeRef != null) {
            LOG.debug("Found the XML file [{}]", fileName);
            return xmlFileNodeRef;
        }
        LOG.debug("Creating new XML file with filename [{}]", fileName);

        Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
        properties.put(ContentModel.PROP_NAME, fileName);

        xmlFileNodeRef = serviceRegistry.getNodeService().createNode(
                parentNodeRef,
                ContentModel.ASSOC_CONTAINS,
                QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, fileName),
                ContentModel.TYPE_CONTENT,
                properties).getChildRef();

        ContentData contentData = (ContentData) serviceRegistry.getNodeService().getProperty(
                xmlFileNodeRef, ContentModel.PROP_CONTENT);
        ContentData newContentData = ContentData.setMimetype(contentData, MimetypeMap.MIMETYPE_XML);
        serviceRegistry.getNodeService().setProperty(xmlFileNodeRef, ContentModel.PROP_CONTENT, newContentData);

        return xmlFileNodeRef;
    }

    @Override
    public void copyAspects(final NodeRef sourceNodeRef, final NodeRef destNodeRef, final Set<QName> aspects) {
        for (QName aspect : aspects) {
            if (serviceRegistry.getNodeService().hasAspect(destNodeRef, aspect)) {
                serviceRegistry.getNodeService().removeAspect(destNodeRef, aspect);
            }
            Map<QName, Serializable> allNodeProps = serviceRegistry.getNodeService().getProperties(sourceNodeRef);
            Map<QName, PropertyDefinition> aspectPropDefs =
                    serviceRegistry.getDictionaryService().getAspect(aspect).getProperties(); // including inherited props
            Map<QName, Serializable> nodeProps = new HashMap<QName, Serializable>(aspectPropDefs.size());
            for (QName propQName : aspectPropDefs.keySet()) {
                Serializable value = allNodeProps.get(propQName);
                if (value != null) {
                    nodeProps.put(propQName, value);
                }
            }
            serviceRegistry.getNodeService().addAspect(destNodeRef, aspect, nodeProps);
        }
    }

    @Override
    public boolean hasSameContent(final NodeRef file1NodeRef, final NodeRef file2NodeRef) {
        ContentReader sourceFileContentReader = serviceRegistry.getFileFolderService().getReader(file1NodeRef);
        ContentReader destinationFileContentReader = serviceRegistry.getFileFolderService().getReader(file2NodeRef);
        String sourceFileHash = computeHash(sourceFileContentReader.getContentInputStream(), MD5_HASH_TYPE);
        String destinationFileHash = computeHash(destinationFileContentReader.getContentInputStream(), MD5_HASH_TYPE);

        return StringUtils.equals(sourceFileHash, destinationFileHash);
    }

    @Override
    public void writeJsonResponse(final WebScriptResponse response, final String json) throws IOException {
        String jsonResponse = null;
        if (json != null) {
            jsonResponse = json;
        } else {
            jsonResponse = EMPTY_JSON;
        }
        response.setContentType(MimetypeMap.MIMETYPE_JSON);
        response.setContentEncoding("UTF-8");
        final int length = jsonResponse.getBytes("UTF-8").length;
        response.addHeader("Content-Length", "" + length);
        response.getWriter().write(jsonResponse);
    }

    /**
     * Compute a Hash code for passed in content
     *
     * @param contentStream the alfresco repository file content stream to compute the hash for
     * @param hashType      the hash algorithm (e.g. md5, sha-1, sha-256, sha-384, sha-512)
     * @return hash code
     */
    private String computeHash(final InputStream contentStream, final String hashType) {
        MessageDigest messageDigest = null;

        try {
            messageDigest = MessageDigest.getInstance(hashType);
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Unable to compute, no hash Algorithm of type [{}]", hashType);
            return null;
        }

        messageDigest.reset();
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = -1;

        try {
            while ((bytesRead = contentStream.read(buffer)) > -1) {
                messageDigest.update(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            LOG.error("Unable to read content stream.", e);
            return null;
        } finally {
            IOUtils.closeQuietly(contentStream);
        }

        byte[] digest = messageDigest.digest();

        return convertByteArrayToHex(digest);
    }

    /**
     * Convert byte array to hash code
     *
     * @param array array of bytes
     * @return hash code
     */
    private String convertByteArrayToHex(final byte[] array) {
        StringBuffer hashValue = new StringBuffer();

        for (byte element : array) {
            String hex = Integer.toHexString(0xFF & element);
            if (hex.length() == 1) {
                hashValue.append('0');
            }
            hashValue.append(hex);
        }

        return hashValue.toString().toUpperCase();
    }

    public byte[] getDocumentContentBytes(NodeRef documentRef) {
        // Get a content reader
        ContentReader contentReader =
                serviceRegistry.getContentService().getReader(documentRef, ContentModel.PROP_CONTENT);
        if (contentReader == null) {
            LOG.error("Content reader was null for [docNodeRef={}]", documentRef);
            return null;
        }

        // Get the document content bytes
        InputStream is = contentReader.getContentInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] documentData = null;

        try {
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = is.read(buf)) > 0) {
                bos.write(buf, 0, len);
            }
            documentData = bos.toByteArray();
        } catch (IOException ioe) {
            LOG.error("Content could not be read: " + ioe.getMessage() + "][docNodeRef={}]", documentRef);
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable e) {
                    LOG.error("Could not close doc content input stream [error={}][docNodeRef={}",
                            e.getMessage(), documentRef);
                }
            }
        }

        return documentData;
    }
}