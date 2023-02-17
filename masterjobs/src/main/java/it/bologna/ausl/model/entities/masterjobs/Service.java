package it.bologna.ausl.model.entities.masterjobs;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.hibernate.type.range.PostgreSQLRangeType;
import com.vladmihalcea.hibernate.type.range.Range;
import it.nextsw.common.annotations.GenerateProjections;
import java.io.Serializable;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author gdm
 */
@TypeDefs({
    @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class),
    @TypeDef(typeClass = PostgreSQLRangeType.class, defaultForType = Range.class)

//    @TypeDef(typeClass = PostgreSQLIntervalType.class, defaultForType = Duration.class),
//    @TypeDef(typeClass = YearMonthDateType.class, defaultForType = YearMonth.class)
})
@Entity
@Table(name = "services", catalog = "internauta", schema = "masterjobs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Cacheable(false)
@GenerateProjections({})
@DynamicUpdate
public class Service implements Serializable {

    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "name")
    private String name;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "start_at")
    @Basic(optional = false)
    @NotNull
    private ZonedDateTime startAt;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "time_interval", columnDefinition = "tstzrange")
//    @Basic(optional = false)
//    @NotNull
    private Range<ZonedDateTime> timeInterval;
        
    @Basic(optional = true)
    @Column(name = "every_seconds")
    private Integer everySeconds;
    
    @Basic(optional = true)
    @Column(name = "every_day_at")
    private LocalTime everyDayAt;
    
    @Basic(optional = true)
    @Column(name = "schedule_on_start")
    private Boolean scheduleOnStart;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "active")
    private Boolean active = true;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "system")
    private Boolean system = false;
    
    @Basic(optional = true)
    @Column(name = "execute_only_on")
    private String executeOnlyOn;
    
    @Basic(optional = true)
    @Column(name = "note")
    private String note;
    
    public Service() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ZonedDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(ZonedDateTime startAt) {
        this.startAt = startAt;
    }

    public Range<ZonedDateTime> getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(Range<ZonedDateTime> timeInterval) {
        this.timeInterval = timeInterval;
    }
    
    public Integer getEverySeconds() {
        return everySeconds;
    }

    public void setEverySeconds(Integer everySeconds) {
        this.everySeconds = everySeconds;
    }

    public LocalTime getEveryDayAt() {
        return everyDayAt;
    }

    public void setEveryDayAt(LocalTime everyDayAt) {
        this.everyDayAt = everyDayAt;
    }

    public Boolean getScheduleOnStart() {
        return scheduleOnStart;
    }

    public void setScheduleOnStart(Boolean scheduleOnStart) {
        this.scheduleOnStart = scheduleOnStart;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getSystem() {
        return system;
    }

    public void setSystem(Boolean system) {
        this.system = system;
    }

    public String getExecuteOnlyOn() {
        return executeOnlyOn;
    }

    public void setExecuteOnlyOn(String executeOnlyOn) {
        this.executeOnlyOn = executeOnlyOn;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
