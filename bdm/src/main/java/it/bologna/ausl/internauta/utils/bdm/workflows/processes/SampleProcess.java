package it.bologna.ausl.internauta.utils.bdm.workflows.processes;

import it.bologna.ausl.internauta.utils.bdm.core.BdmProcess;
import it.bologna.ausl.internauta.utils.bdm.core.Step;
import it.bologna.ausl.internauta.utils.bdm.core.Task;
import it.bologna.ausl.internauta.utils.bdm.workflows.tasks.SampleTask;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

/**
 *
 * @author andrea
 */
//@Embeddable
public class SampleProcess extends BdmProcess implements Serializable{

//    @Override
    public void init(Map<String, Object> parameters) {
        setContext(parameters);
        Step s = new Step("SampleStep", "Sample Process", Step.StepLogic.SEQ, Arrays.asList(Step.StepLogic.SEQ, Step.StepLogic.ALL));
//        addStep(s);
        Task t = new SampleTask();
        s.addTask(t);
        addStep(s);
    }

//    @Override
    public String getProcessVersion() {
        return "0.1";
    }

//    @Override
    public String getProcessType() {
        return this.getClass().toString();
    }

    @Override
    public void setProcessType(String type) {

    }

}
