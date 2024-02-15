package it.bologna.ausl.internauta.utils.masterjobs;

/**
 *
 * @author gdm
 */
public class MasterjobsWorkingObject {
    private String id;
    private String type;

    public MasterjobsWorkingObject(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
