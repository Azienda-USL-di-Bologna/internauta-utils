package it.bologna.ausl.internauta.utils.bdm.workflows.tasks;

import it.bologna.ausl.internauta.utils.bdm.core.BdmProcess.BdmStatus;
import it.bologna.ausl.internauta.utils.bdm.core.Context;
import it.bologna.ausl.internauta.utils.bdm.core.Result;
import it.bologna.ausl.internauta.utils.bdm.core.Task;
import it.bologna.ausl.internauta.utils.bdm.utilities.Bag;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * @author andrea
 */
public class SampleTask extends Task {
    private final Logger log = LoggerFactory.getLogger(SampleTask.class);
    
    @Override
    public String getTaskType() {
        return "SampleTask";
    }

    @Override
    public Result execute(Bag runningContext, Bag context, Bag params) {
        status = BdmStatus.RUNNING;
        log.info("executing task " + getTaskType() + "...");
        Boolean ok = true;
        if (params.get("ok") != null) {
            ok = (Boolean) params.get("ok");
        }
        if (ok) {
            status = BdmStatus.FINISHED;
        } else {
            status = BdmStatus.RUNNING;
        }
        log.info("task " + getTaskType() + " executed");
        return new Result(status, null, null);
    }

    @Override
    public void stepIn(Context c, Bag p) {
    }

    @Override
    public String getTaskVersion() {
        return "0001";
    }

}
