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

import org.activiti.engine.delegate.DelegateExecution;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.workflow.WorkflowInstance;
import org.alfresco.service.cmr.workflow.WorkflowTask;
import org.alfresco.service.namespace.QName;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Activiti Workflow specific helper methods.
 *
 * @author martin.bergljung@marversolutions.org
 * @version 1.0
 */
public interface AlfrescoWorkflowUtilsService {

    /**
     * Returns the username for the person that initiated/started the workflow with passed in ID.
     *
     * @param workflowId the workflow instance identifier
     * @return the username for the person that initiated the workflow
     */
    String getInitiator(final String workflowId);

    /**
     * Get all assigned and pooled tasks for workflow instances with passed in workflow definition.
     *
     * @param workflowDefinitionName the activiti workflow definition name
     * @return a list of assigned and pooled tasks for process instances with passed in process definition
     */
    List<WorkflowTask> getAssignedAndPooledTasksForProcessDefinition(final String workflowDefinitionName);

    /**
     * Get all completed workflow instances for passed in workflow definition
     *
     * @param workflowDefId the workflow definition that we want to search for completed workflows for
     * @return a list of all completed workflow instances
     */
    List<WorkflowInstance> getCompletedWorkflows(final String workflowDefId);

    /**
     * Get all active workflow instances for passed in workflow definition name
     * (e.g. activiti$bestpub-publishing-process), regardless of the workflow definition version/id
     * (e.g. activiti$bestpub-publishing-process:3:4608) used by the instances.
     *
     * @param workflowDefinitionName the workflow definition name such as 'activiti$bestpub-publishing-process'
     * @return all workflow instances that are active for the workflow definition name
     */
    List<WorkflowInstance> getActiveWorkflows(final String workflowDefinitionName);

    /**
     * Get all workflow instances (both active and completed) for passed in workflow definition name
     * (e.g. activiti$bestpub-publishing-process), regardless of the workflow definition version/id
     * (e.g. activiti$bestpub-publishing-process:3:4608) used by the instances.
     *
     * @param workflowDefinitionName the workflow definition name such as 'activiti$bestpub-publishing-process'
     * @return all workflow instances that are active or completed for the workflow definition name
     */
    public List<WorkflowInstance> getWorkflows(final String workflowDefinitionName);

    /**
     * Get the value of an Activiti Process Instance variable.
     * These are variables set with for example "execution.setVariable"
     *
     * @param workflowInstance the workflow instance (e.g. ID for it would be something like activiti$23456)
     * @param varName          a process variable name
     * @return the value of the process variable, need to be casted to the correct type
     */
    public Object getProcessVariable(final WorkflowInstance workflowInstance, final String varName);

    /**
     * Get the String value of an Activiti Process Instance variable.
     *
     * @param workflowInstance the workflow instance (e.g. e.g. ID for it would be something like activiti$23456)
     * @param variableName     a process variable name
     * @return the String value of an Activiti Process Instance variable, or "" empty string if process variable is not set
     */
    public String getStringProcessVariable(final WorkflowInstance workflowInstance, final String variableName);

    /**
     * Get the boolean value of an Activiti Process Instance variable.
     *
     * @param workflowInstance the workflow instance (e.g. e.g. ID for it would be something like activiti$23456)
     * @param variableName     a process variable name
     * @return the boolean value of an Activiti Process Instance variable
     */
    public boolean getBooleanProcessVariable(final WorkflowInstance workflowInstance, final String variableName);

    /**
     * Get the Collection value of an Activiti Process Instance variable
     * 
     * @param workflowInstance
     * @param variableName
     * @return
     */
    public Collection<String> getCollectionProcessVariable(final WorkflowInstance workflowInstance, final String variableName);
    
    public Integer getIntProcessVariable(final WorkflowInstance workflowInstance, final String variableName);
    
    /**
     * Set the value of an Activiti Process Instance variable.
     * These are variables set with for example "execution.setVariable"
     *
     * @param workflowInstanceId the workflow instance identifier (e.g. activiti$23456)
     * @param varName            a process variable name
     * @param value              value to set
     */
    void setProcessVariable(final String workflowInstanceId, final String varName, final Object value);

    /**
     * Returns all workflow tasks that the {@link NodeRef} is part of
     *
     * @param node a node reference for a content file or folder
     * @return all workflow task associated with that particular Node
     */
    List<WorkflowInstance> getNodeWorkflows(final NodeRef node);

    /**
     * Get workflow instance related to passed in ISBN number.
     *
     * @param workflowDefinitionName the workflow definition name that the workflow instance has been started from,
     *                               such as 'activiti$bestpub-publishing-process'
     * @param isbn          the ISBN-13 number to lookup workflow instance for (e.g. 9780203807217)
     * @return WorkflowInstance object if workflow instance with var relatedISBN = isbn is found
     */
    public WorkflowInstance getWorkflowInstanceForIsbn(final String workflowDefinitionName, final String isbn);

    /**
     * Get a list of package items (i.e. files and folders associated with bpm_package) for workflow instance
     * associated with passed in execution context.
     *
     * @param exec delegated execution context
     * @return a list of files and folder node references that are attached to the workflow instance
     */
    List<NodeRef> getWorkflowPackageItems(DelegateExecution exec);

    /**
     * Get workflow package nodeRef.
     *
     * @param exec delegated execution context
     * @return workflow package nodeRef
     */
    NodeRef getWorkflowPackageNodeRef(DelegateExecution exec);

    /**
     * Prepare and start a workflow instance.
     *
     * @param workflowDefId       the process definition identifier (e.g. activiti$adHoc)
     * @param packageFileNodeRefs the files that should be attached to the bpm_package variable
     * @param properties          the properties that the worklflow instance should be started with
     * @return the workflow instance object for the newly started workflow, or null if it could not be started
     */
    public WorkflowInstance startWorkflowInstance(final String workflowDefId,
                                                  final List<NodeRef> packageFileNodeRefs,
                                                  final Map<QName, Serializable> properties);

    /**
     * Get the passed in property as a list of Strings.
     * <p/>
     * Either the property is already a list of strings,
     * or it's a string with values separated by passed in separator (for example "|")
     *
     * @param propertyValue object representing the property value
     * @param separator the separator to use when splitting a string with values
     * @return
     */
    public List<String> getWorkflowPropertyAsList(Object propertyValue, final String separator);

    /**
     * Get a collection from a property.
     * <p/>
     * Either the property is already a collection of strings,
     * or it's a comma separated string which will be converted into a collection.
     *
     * @param propertyValue object representing the property value
     * @return
     */
    public Collection<String> getWorkflowPropertyAsCollection(Object propertyValue);
}
