package WasserkarteInfoExporter.helper;

public class Hydrant {

    private String id;

    private String name;

    private Double longitude;

    private Double latitude;

    private HydrantType hydrantType;

    private Long diameter;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public HydrantType getHydrantType() {
        return hydrantType;
    }

    public void setHydrantType(HydrantType hydrantType) {
        this.hydrantType = hydrantType;
    }

    public Long getDiameter() {
        return diameter;
    }

    public void setDiameter(Long diameter) {
        this.diameter = diameter;
    }

    @Override
    public String toString() {
        return "Id:" + getId() + ",Name:" + getName() + ", Type:" + getHydrantType() + ",Diameter:" + getDiameter();
    }
}
