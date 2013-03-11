package be.stacktrace.activiti.dynamicprocess;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import junit.framework.Assert;

import org.activiti.bpmn.BpmnAutoLayout;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.ActivitiRule;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;

public class DynamicActivitiProcessTest {
  
  @Rule
  public ActivitiRule activitiRule = new ActivitiRule();
  
  @Test
  public void testDynamicDeploy() throws Exception {
    // 1. Build up the model from scratch
    BpmnModel model = new BpmnModel();
    Process process = new Process();
    model.addProcess(process);
    process.setId("my-process");

    process.addFlowElement(createStartEvent());
    process.addFlowElement(createUserTask("task1", "First task", "fred"));
    process.addFlowElement(createUserTask("task2", "Second task", "john"));
    process.addFlowElement(createEndEvent());
    
    process.addFlowElement(createSequenceFlow("start", "task1"));
    process.addFlowElement(createSequenceFlow("task1", "task2"));
    process.addFlowElement(createSequenceFlow("task2", "end"));
    
    // 2. Generate graphical information
    new BpmnAutoLayout(model).execute();
    
    // 3. Deploy the process to the engine
    Deployment deployment = activitiRule.getRepositoryService().createDeployment()
        .addBpmnModel("dynamic-model.bpmn", model).name("Dynamic process deployment").deploy();
    
    // 4. Start a process instance
    ProcessInstance processInstance = activitiRule.getRuntimeService()
        .startProcessInstanceByKey("my-process");

    // 5. Check if task is available
    List<Task> tasks = activitiRule.getTaskService().createTaskQuery()
      .processInstanceId(processInstance.getId()).list();
    
    Assert.assertEquals(1, tasks.size());
    Assert.assertEquals("First task", tasks.get(0).getName());
    Assert.assertEquals("fred", tasks.get(0).getAssignee());
    
    // 6. Save process diagram to a file  
    InputStream processDiagram = activitiRule.getRepositoryService().getProcessDiagram(processInstance.getProcessDefinitionId());
    FileUtils.copyInputStreamToFile(processDiagram, new File("target/diagram.png"));
    
    // 7. Save resulting BPMN xml to a file
    InputStream processBpmn = activitiRule.getRepositoryService().getResourceAsStream(deployment.getId(), "dynamic-model.bpmn");
    FileUtils.copyInputStreamToFile(processBpmn, new File("target/process.bpmn20.xml"));
  }
  
  protected UserTask createUserTask(String id, String name, String assignee) {
    UserTask userTask = new UserTask();
    userTask.setName(name);
    userTask.setId(id);
    userTask.setAssignee(assignee);
    return userTask;
  }
  
  protected SequenceFlow createSequenceFlow(String from, String to) {
    SequenceFlow flow = new SequenceFlow();
    flow.setSourceRef(from);
    flow.setTargetRef(to);
    return flow;
  }
  
  protected StartEvent createStartEvent() {
    StartEvent startEvent = new StartEvent();
    startEvent.setId("start");
    return startEvent;
  }
  
  protected EndEvent createEndEvent() {
    EndEvent endEvent = new EndEvent();
    endEvent.setId("end");
    return endEvent;
  }
}