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

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.webscripts.WebScriptResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * Helper methods when interacting with the Alfresco repository, such as searching for a node via display path.
 *
 * @author martin.bergljung@marversolutions.org
 * @version 1.0
 */
public interface AlfrescoRepoUtilsService {

    /**
     * @return The {@link NodeRef} of companyHome.
     */
    NodeRef getCompanyHome();

    /**
     * @return The {@link NodeRef} of the Data Dictionary
     */
    NodeRef getDataDictionary();

    /**
     * Resolves a node reference in the 'workspace://SpacesStore' via its display path.
     *
     * @param path the display path excluding /Company Home and store,
     *             such as for example "Data Dictionary/Email Templates"
     * @return the node reference for the display path, or null if not found
     */
    NodeRef getNodeByDisplayPath(String path);

    /**
     * Get a child node by name for passed in parent node.
     *
     * @param parent search for child nodes of this parent
     * @param name   the name of the child node we are looking for
     * @return the node reference for the child node if found, otherwise null
     */
    NodeRef getChildByName(NodeRef parent, String name);

    /**
     * Gets, or creates, a folder node based on passed in folder name and parent folder node reference.
     *
     * @param parentNodeRef the parent folder node reference that should contain the new folder/existing folder
     * @param name          the folder name we are looking for
     * @return a folder node reference
     */
    NodeRef getOrCreateFolder(NodeRef parentNodeRef, String name);

    /**
     * Creates a folder node based on passed in folder name, folder type, and parent folder node reference.
     *
     * @param parentNodeRef the parent folder node reference that should contain the new folder
     * @param name          the folder name
     * @param type          the QName for the type that the new node should have
     * @return the node reference for the new node
     */
    NodeRef createFolder(NodeRef parentNodeRef, String name, QName type);

    /**
     * Gets, or creates, a file node in the passed in parent folder with passed in filename and content.
     *
     * @param parentFolderNodeRef folder to add the file to, or where it exists
     * @param filename            the name of the new file
     * @param mimeType            the mimetype of the file (e.g. plain/txt)
     * @param content             the content string that will make up the physical content of the file
     * @return a file node reference for an existing file or for the newly created file,
     * or null if file could not be created
     */
    NodeRef getOrCreateFile(NodeRef parentFolderNodeRef, String filename,
                                   String mimeType, String content);

    /**
     * Creates a file node with passed in filename as a sub-folder to passed in parent folder.
     * The file node will have only metadata, no content.
     *
     * @param parentFolderNodeRef folder to add the file to
     * @param filename            the filename that the new file node should have
     * @return a file node reference for the newly created file, or null if file already exists or could not be created
     */
    NodeRef createFileMetadataOnly(NodeRef parentFolderNodeRef, String filename);

    /**
     * Creates a file node in the passed in parent folder with filename from passed in File object .
     * Content bytes from the File object will be attached to File node.
     *
     * @param parentFolderNodeRef folder to add the file to
     * @param file                the object with filename and content bytes
     * @return a file node reference for the newly created file, or null if file already exists or could not be created
     */
    NodeRef createFile(NodeRef parentFolderNodeRef, File file);
    NodeRef createFile(NodeRef parentFolderNodeRef, String filename, InputStream fileInputStream);

    /**
     * Creates a file node in the passed in parent folder with passed in filename and content.
     *
     * @param parentFolderNodeRef folder to add the file to
     * @param filename            the name of the new file
     * @param mimeType            the mimetype of the file (e.g. plain/txt)
     * @param content             the content string that will make up the physical content of the file
     * @return a file node reference for the newly created file, or null if file already exists or could not be created
     */
    NodeRef createFile(NodeRef parentFolderNodeRef, String filename,
                              String mimeType, String content);

    /**
     * Does a Lucene query with passed in query expression.
     * If more than one node matches then the first one is returned.
     *
     * @param query Lucene query matching one node
     * @return the Alfresco node reference for the matching node, or null if no nodes matched
     */
    NodeRef searchOne(String query);

    /**
     * Does a Lucene query with passed in query expression.
     *
     * @param query Lucene query matching one node
     * @return the Alfresco node references that matched query, or empty list
     */
    List<NodeRef> search(String query);

    /**
     * Returns true if the user 'username' is a part of the group 'groupName'
     *
     * @param username  username for the user
     * @param groupName the name of the group to search in
     * @return true if the user with 'username' is a part of the group 'groupName'
     */
    boolean isPartOfGroup(String username, String groupName);

    /**
     * Create a file with xml mimetype.
     *
     * @param parentNodeRef
     * @param fileName
     * @return nodeRef
     */
    NodeRef getOrCreateXMLFileMetadata(NodeRef parentNodeRef, String fileName);

    /**
     * Finds a node from a path in the following format:
     * /app:company_home/app:dictionary/cm:BOPP/cm:Incoming/cm:Content/cm:Logs
     *
     * @param path Path to file/folder with namespace prefixes (eg: "cm:")
     * @return NodeRef of the node at the path specified or null if no or multiple nodes are found.
     */
    NodeRef getNodeByXPath(String path);

    /**
     * Copy and overwrite the set of aspects from source to destination.
     *
     * @param sourceNodeRef
     * @param destNodeRef
     * @param aspects
     */
    void copyAspects(NodeRef sourceNodeRef, NodeRef destNodeRef, Set<QName> aspects);

    /**
     * Compares the bytes of two content files and returns true if they are the same.
     * Uses MD5 hash to determine if they are equal.
     *
     * @param file1NodeRef the first file node reference
     * @param file2NodeRef the second file node reference
     * @return returns true if both these files has the same content bytes, false if they have different content
     */
    boolean hasSameContent(NodeRef file1NodeRef, NodeRef file2NodeRef);

    /**
     * Writes JSON string to response setting correct headers.
     *
     * @param response WebScript response
     * @param json     String representation of the JSON Object that will be written in WebScript response
     * @throws IOException
     */
    void writeJsonResponse(WebScriptResponse response, String json) throws IOException;

    /**
     * Get the content bytes for the document with passed in node reference.
     *
     * @param documentRef the node reference for the document we want the content bytes for
     * @return a byte array containing the document content or null if not found
     */
    byte[] getDocumentContentBytes(NodeRef documentRef);
}