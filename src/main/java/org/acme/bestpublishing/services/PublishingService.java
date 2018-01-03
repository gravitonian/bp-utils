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

/**
 * Service used to create and store the EPub artifact that will be picked up by the fictive publishing system
 * and then delivered to the Website.
 *
 * Called from the Publishing DocLib action.
 *
 * @author martin.bergljung@marversolutions.org
 * @version 1.0
 */
public interface PublishingService {
    /**
     * Write content of /Company Home/Sites/book-management/documentLibrary/{year}/{isbn}
     * folder directly to EPub 3 file on disk (basically a ZIP).
     * The {isbn}.epub file will be stored in the /Company Home/Sites/book-library/documentLibrary/{year} folder.
     * It will also be stored in a local directory configured in alfresco-globals.properties,
     * for example /alf_data_dev/bestpub/epubs.
     *
     * @param isbnFolderNodeRef the ISBN folder node reference for the book that we want to publish as an EPub
     * @return true if the EPub was written successfully to disk and site, false if there was an error
     */
    public boolean createAndStoreEPubArtifact(NodeRef isbnFolderNodeRef);
}