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
package org.acme.bestpublishing.model;

import org.alfresco.service.namespace.QName;

/**
 * Best Publishing Workflow Model Java wrapper class.
 *
 * @author martin.bergljung@marversolutions.org
 * @version 1.0
 */
public class BestPubWorkflowModel {
    // Workflow definition identifier
    public static final String BESTPUB_PUBLISHING_WORKFLOW_NAME = "activiti$bestpub-publishing-process";

    // BestPub Workflow model namespace
    public final static String NAMESPACE_URI = "http://www.acme.org/model/workflow/content/1.0";
    public final static String NAMESPACE_PREFIX = "bestpubw";

    /**
     * Workflow model properties QNames
     */
    public static final QName PROP_PUBLISHING_DATE = QName.createQName(NAMESPACE_URI, "publishingDate");

    // Passing around content model properties as process variables requires
    // the use of _ between namespace and local prop name

    // Process variables that are part of the BestPub Workflow Model XML,
    // defined separately as they got underscore between namespace and local name.
    public static final String VAR_PUBLISHING_DATE = NAMESPACE_PREFIX + "_publishingDate";

    // Process variables not in the BestPub Workflow Model XML but instead in
    // BestPub Content Model XML (i.e. bestpub: namespace required instead of bestpubw:)
    public static final String VAR_ISBN = BestPubContentModel.NAMESPACE_PREFIX + "_ISBN";
    public static final String VAR_BOOK_TITLE = BestPubContentModel.NAMESPACE_PREFIX + "_bookTitle";
    public static final String VAR_BOOK_GENRE = BestPubContentModel.NAMESPACE_PREFIX + "_bookGenre";
    public static final String VAR_BOOK_AUTHORS = BestPubContentModel.NAMESPACE_PREFIX + "_bookAuthors";
    public static final String VAR_BOOK_NR_OF_CHAPTERS = BestPubContentModel.NAMESPACE_PREFIX + "_nrOfChapters";
    public static final String VAR_BOOK_NR_OF_PAGES = BestPubContentModel.NAMESPACE_PREFIX + "_nrOfPages";

    // Process variables not in any BestPub Model XML (i.e. no namespace required)
    public static final String VAR_ALL_METADATA = "allMetadata";
    public static final String VAR_BOOK_INFO = "bookInfo";
    public static final String VAR_CHAPTER_LIST = "chapterList";
    public static final String VAR_CONTENT_CHAPTER_MATCHING_OK = "contentChapterMatchingOk";
    public static final String VAR_CONTENT_FOUND = "contentFound";
    public static final String VAR_CONTENT_ERROR_FOUND = "contentErrorFound";
    public static final String VAR_METADATA_FOUND = "metadataFound";
    public static final String VAR_CURRENT_EA_USER = "currentEAUser";
    public static final String VAR_INTERRUPT_T1_TIMER_DURATION = "InterruptT1TimerDuration";
    public static final String VAR_WAIT_2_CHECK_CONTENT_TIMER_DURATION = "Wait2Check4ContentTimerDuration";
    public static final String VAR_METADATA_CHAPTER_MATCHING_OK = "metadataChapterMatchingOk";
    public static final String VAR_METADATA_COMPLETE = "metadataComplete";
    public static final String VAR_CHAPTER_FOLDER_HIERARCHY_EXISTS = "chapterFolderHierarchyExists";
    public static final String VAR_TASK_T5_EXECUTED = "TaskT5Executed";

    // Properties that matches process variables when starting the workflow,
    // they don't have a namespace as they are process variables.
    public static final String NO_NAMESPACE = "{}";
    public static final QName PROP_ALL_METADATA = QName.createQName(NO_NAMESPACE + VAR_ALL_METADATA);
    public static final QName PROP_CONTENT_FOUND = QName.createQName(NO_NAMESPACE + VAR_CONTENT_FOUND);
    public static final QName PROP_CONTENT_ERROR_FOUND = QName.createQName(NO_NAMESPACE + VAR_CONTENT_ERROR_FOUND);
    public static final QName PROP_METADATA_CHAPTER_MATCHING_OK =
            QName.createQName(NO_NAMESPACE + VAR_METADATA_CHAPTER_MATCHING_OK);
    public static final QName PROP_CHAPTER_FOLDER_HIERARCHY_EXISTS =
            QName.createQName(NO_NAMESPACE + VAR_CHAPTER_FOLDER_HIERARCHY_EXISTS);
    public static final QName PROP_INTERRUPT_T1_TIMER_DURATION =
            QName.createQName(NO_NAMESPACE + VAR_INTERRUPT_T1_TIMER_DURATION);
    public static final QName PROP_WAIT_2_CHECK_CONTENT_TIMER_DURATION =
            QName.createQName(NO_NAMESPACE + VAR_WAIT_2_CHECK_CONTENT_TIMER_DURATION);

    public static QName qname(final String qname) {
        return QName.createQName(NAMESPACE_URI, qname);
    }
}