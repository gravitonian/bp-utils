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

import org.acme.bestpublishing.model.BestPubWorkflowModel;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.history.HistoricVariableInstanceQuery;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.workflow.BPMEngineRegistry;
import org.alfresco.repo.workflow.WorkflowModel;
import org.alfresco.repo.workflow.WorkflowNotificationUtils;
import org.alfresco.repo.workflow.activiti.ActivitiScriptNode;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.workflow.*;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import java.io.Serializable;
import java.util.*;


/**
 * Activiti Workflow specific helper methods implementation.
 * <p>
 * Note. these are non-transactional call requiring an existing transaction to be in place.
 * Note2. proper permission checks also need to be in place before making calls.
 *
 * @author martin.bergljung@marversolutions.org
 * @version 1.0
 */
public class AlfrescoWorkflowUtilsServiceImpl implements AlfrescoWorkflowUtilsService, BeanFactoryAware {
    private static final Logger LOG = LoggerFactory.getLogger(AlfrescoWorkflowUtilsServiceImpl.class);

    private static final String ACTIVITI_RUNTIME_SERVICE_SPRING_BEAN_ID = "activitiRuntimeService";
    private static final String ACTIVITI_HISTORY_SERVICE_SPRING_BEAN_ID = "activitiHistoryService";

    /*
     * Spring Bean Factory within which the registry lives
     */
    private BeanFactory beanFactory;

    /**
     * Alfresco Services
     */
    private ServiceRegistry serviceRegistry;

    /**
     * Activiti services
     */
    private RuntimeService activitiRuntimeService;
    private HistoryService activitiHistoryService;

    /*
     * Spring DI
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * Used from test class
     *
     * @param activitiRuntimeService
     */
    public void setActivitiRuntimeService(RuntimeService activitiRuntimeService) {
        this.activitiRuntimeService = activitiRuntimeService;
    }

    @Override
    public String getInitiator(String workflowId) {
        NodeRef initiatorPersonNodeRef =
                serviceRegistry.getWorkflowService().getWorkflowById(workflowId).getInitiator();
        return (String) serviceRegistry.getNodeService().getProperty(initiatorPersonNodeRef, ContentModel.PROP_USERNAME);
    }

    @Override
    public List<WorkflowTask> getAssignedAndPooledTasksForProcessDefinition(String workflowDefinitionName) {
        // Setup query for active tasks in progress
        WorkflowTaskQuery query = new WorkflowTaskQuery();
        query.setTaskState(WorkflowTaskState.IN_PROGRESS);
        query.setWorkflowDefinitionName(workflowDefinitionName);
        query.setOrderBy(new WorkflowTaskQuery.OrderBy[]{
                WorkflowTaskQuery.OrderBy.TaskCreated_Desc, WorkflowTaskQuery.OrderBy.TaskActor_Asc});

        // Do the query
        boolean sameSession = true;
        List<WorkflowTask> queryTasks = new ArrayList<WorkflowTask>();
        queryTasks.addAll(serviceRegistry.getWorkflowService().queryTasks(query, sameSession));

        return queryTasks;
    }

    @Override
    public List<WorkflowInstance> getNodeWorkflows(NodeRef node) {
        return serviceRegistry.getWorkflowService().getWorkflowsForContent(node, true);
    }

    @Override
    public List<WorkflowInstance> getCompletedWorkflows(String workflowDefId) {
        return serviceRegistry.getWorkflowService().getCompletedWorkflows();
    }

    @Override
    public List<WorkflowInstance> getActiveWorkflows(String workflowDefinitionName) {
        List<WorkflowInstance> activeWorkflows = new ArrayList<>();
        List<WorkflowInstance> allActiveWorkflows = serviceRegistry.getWorkflowService().getActiveWorkflows();
        for (WorkflowInstance activeWorkflowInstance : allActiveWorkflows) {
            if (activeWorkflowInstance.getDefinition().getName().equals(workflowDefinitionName)) {
                activeWorkflows.add(activeWorkflowInstance);
            }
        }

        return activeWorkflows;
    }

    @Override
    public List<WorkflowInstance> getWorkflows(String workflowDefinitionName) {
        List<WorkflowInstance> workflows = new ArrayList<>();
        List<WorkflowInstance> allWorkflows = serviceRegistry.getWorkflowService().getWorkflows();
        for (WorkflowInstance workflowInstance : allWorkflows) {
            if (workflowInstance.getDefinition().getName().equals(workflowDefinitionName)) {
                workflows.add(workflowInstance);
            }
        }

        return workflows;
    }

    @Override
    public Object getProcessVariable(WorkflowInstance workflowInstance, String variableName) {
        if (workflowInstance.isActive()) {
            String executionId = BPMEngineRegistry.getLocalId(workflowInstance.getId());
            return getActivitiRuntimeService().getVariable(executionId, variableName);
        } else {
            HistoricVariableInstanceQuery hviq = getActivitiHistoryService().createHistoricVariableInstanceQuery().
                    processInstanceId(workflowInstance.getId()).variableName(variableName);
            HistoricVariableInstance hvi = hviq.singleResult();
            if (hvi != null) {
                return hvi.getValue();
            } else {
                LOG.debug("Process variable {} is not set for process instance [{}]", 
                        variableName, workflowInstance.getId());
                return null;
            }
            //return .singleResult().getValue();
        }
    }

    @Override
    public String getStringProcessVariable(WorkflowInstance workflowInstance, String variableName) {
        Object variableObject = getProcessVariable(workflowInstance, variableName);
        String varString = "";
        if (variableObject != null) {
            varString = (String) variableObject;
        }
        return varString;
    }

    @Override
    public boolean getBooleanProcessVariable(WorkflowInstance workflowInstance, String variableName) {
        Object variableObject = getProcessVariable(workflowInstance, variableName);
        boolean variableBoolean = false;
        if (variableObject != null) {
            variableBoolean = (boolean) variableObject;
        }
        return variableBoolean;
    }

    @Override
    public Collection<String> getCollectionProcessVariable(WorkflowInstance workflowInstance,
                                                           String variableName) {
        Object variableObject = getProcessVariable(workflowInstance, variableName);
        Collection<String> collection = null;
        if (variableObject != null && variableObject instanceof Collection<?>) {
            collection = (Collection<String>) variableObject;
        }
        return collection;
    }

    @Override
    public Integer getIntProcessVariable(WorkflowInstance workflowInstance, String variableName) {
        Object variableObject = getProcessVariable(workflowInstance, variableName);
        Integer intg = null;
        if (variableObject != null && variableObject instanceof Collection<?>) {
            intg = (Integer) variableObject;
        }
        return intg;
    }

    @Override
    public void setProcessVariable(String workflowInstanceId, String varName, Object value) {
        String executionId = BPMEngineRegistry.getLocalId(workflowInstanceId);
        getActivitiRuntimeService().setVariable(executionId, varName, value);
    }

    @Override
    public WorkflowInstance getWorkflowInstanceForIsbn(String workflowDefinitionName, String isbn) {
        WorkflowInstance workflowInstanceForIsbn = null;
        List<WorkflowInstance> wfInstances = getWorkflows(workflowDefinitionName);
        for (WorkflowInstance workflowInstance : wfInstances) {
            String varIsbn = getStringProcessVariable(workflowInstance, BestPubWorkflowModel.VAR_RELATED_ISBN);
            if (StringUtils.equals(isbn, varIsbn)) {
                workflowInstanceForIsbn = workflowInstance;
                break;
            }
        }
        return workflowInstanceForIsbn;
    }

    @Override
    public WorkflowInstance startWorkflowInstance(String workflowDefId, Map<QName, Serializable> properties) {
        List<NodeRef> packageFileNodeRefs = new ArrayList<>();
        return startWorkflowInstance(workflowDefId, packageFileNodeRefs, properties);
    }

    @Override
    public WorkflowInstance startWorkflowInstance(String workflowDefId,
                                                  List<NodeRef> packageFileNodeRefs,
                                                  Map<QName, Serializable> properties) {
        // Setup workflow package with files (i.e. setup bpm_package)
        NodeRef workflowPackage = serviceRegistry.getWorkflowService().createPackage(null);
        for (NodeRef packageFileNodeRef : packageFileNodeRefs) {
            ChildAssociationRef childAssoc = serviceRegistry.getNodeService().getPrimaryParent(packageFileNodeRef);
            serviceRegistry.getNodeService().addChild(workflowPackage, packageFileNodeRef, 
                    WorkflowModel.ASSOC_PACKAGE_CONTAINS, childAssoc.getQName());
        }
        properties.put(WorkflowModel.ASSOC_PACKAGE, workflowPackage);

        // Start the workflow
        // Get latest deployed definition such as "activiti$bestpub-publishing-process:34:9311" from
        // "activiti$bestpub-publishing-process"
        WorkflowDefinition workflowDefinition = 
                this.serviceRegistry.getWorkflowService().getDefinitionByName(workflowDefId);
        WorkflowPath workflowPath = 
                this.serviceRegistry.getWorkflowService().startWorkflow(workflowDefinition.getId(), properties);

        // Complete the start task, otherwise it will not start
        String workflowId = workflowPath.getInstance().getId();
        WorkflowTask startTask = serviceRegistry.getWorkflowService().getStartTask(workflowId);
        try {
            // Only end the start task if it is still active
            if (startTask.getPath().isActive()) {
                serviceRegistry.getWorkflowService().endTask(startTask.getId(), null);
            }
        } catch (RuntimeException e) {
            LOG.error("Could not start workflow for ISBN " + properties.get(BestPubWorkflowModel.PROP_RELATED_ISBN) +
                    " [workflowDefId=" + workflowDefId + "][packageFileSize=" + packageFileNodeRefs.size() + "]", e);
            return null;
        }

        LOG.debug("Started workflow for ISBN {} [workflowDefId={}][workflowInstanceId={}]packageFileSize={}]",
                new Object[]{properties.get(BestPubWorkflowModel.PROP_RELATED_ISBN), workflowDefId,
                        workflowPath.getInstance().getId(), packageFileNodeRefs.size()});

        return workflowPath.getInstance();
    }

    @Override
    public List<NodeRef> getWorkflowPackageItems(DelegateExecution exec) {
        List<NodeRef> packageItemNodeRefs = new ArrayList<NodeRef>();
        Object workflowPackage = exec.getVariable(WorkflowNotificationUtils.PROP_PACKAGE);
        if (workflowPackage == null) {
            LOG.debug("Workflow package is not available (null) for process [definition={}][instance={}]",
                    exec.getProcessDefinitionId(), exec.getProcessInstanceId());
        } else {
            ActivitiScriptNode activitiScriptNode = (ActivitiScriptNode) workflowPackage;
            NodeRef workflowPackageNodeRef = activitiScriptNode.getNodeRef();
            List<ChildAssociationRef> packageItemChildAssociations = 
                    serviceRegistry.getNodeService().getChildAssocs(workflowPackageNodeRef);
            for (ChildAssociationRef packageItemChildAssociation : packageItemChildAssociations) {
                packageItemNodeRefs.add(packageItemChildAssociation.getChildRef());
            }
        }

        if (packageItemNodeRefs.isEmpty()) {
            LOG.debug("No items were found in workflow package for process [definition={}][instance={}]",
                    exec.getProcessDefinitionId(), exec.getProcessInstanceId());
        } else {
            LOG.debug("Workflow package for process contains [{}] items [definition={}][instance={}]",
                    new Object[]{packageItemNodeRefs.size(), exec.getProcessDefinitionId(), exec.getProcessInstanceId()});
        }

        return packageItemNodeRefs;
    }

    @Override
    public NodeRef getWorkflowPackageNodeRef(DelegateExecution exec) {
        Object workflowPackage = exec.getVariable(WorkflowNotificationUtils.PROP_PACKAGE);
        if (workflowPackage == null) {
            LOG.debug("Workflow package is not available (null) for process [definition={}][instance={}]",
                    exec.getProcessDefinitionId(), exec.getProcessInstanceId());
            return null;
        } else {
            ActivitiScriptNode activitiScriptNode = (ActivitiScriptNode) workflowPackage;
            return activitiScriptNode.getNodeRef();
        }
    }

    @Override
    public List<String> getWorkflowPropertyAsList(Object propertyValue, String separator) {
        if (propertyValue instanceof String) {
            return Arrays.asList(((String) propertyValue).split(separator));
        } else if (propertyValue instanceof List) {
            return (List<String>) propertyValue;
        } else {
            return null;
        }
    }

    @Override
    public Collection<String> getWorkflowPropertyAsCollection(Object propertyValue) {
        if (propertyValue instanceof String) {
            return Arrays.asList(((String) propertyValue).split(","));
        } else if (propertyValue instanceof Set) {
            return (Set<String>) propertyValue;
        } else if (propertyValue instanceof Collection) {
            return (Collection<String>) propertyValue;
        } else {
            return null;
        }
    }

    /**
     * Get the Activiti Workflow engine runtime service
     *
     * @return Activiti Runtime Service
     */
    private RuntimeService getActivitiRuntimeService() {
        if (this.activitiRuntimeService == null) {
            this.activitiRuntimeService =
                    beanFactory.getBean(ACTIVITI_RUNTIME_SERVICE_SPRING_BEAN_ID, RuntimeService.class);
        }
        return this.activitiRuntimeService;
    }

    /**
     * Get the Activiti Workflow engine History service
     *
     * @return Activiti History Service
     */
    private HistoryService getActivitiHistoryService() {
        if (this.activitiHistoryService == null) {
            this.activitiHistoryService =
                    beanFactory.getBean(ACTIVITI_HISTORY_SERVICE_SPRING_BEAN_ID, HistoryService.class);
        }
        return this.activitiHistoryService;
    }


}