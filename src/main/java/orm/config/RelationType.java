package orm.config;

public enum RelationType {
    ONE_TO_MANY,
    MANY_TO_ONE;

    public boolean isForeignKeyRight() {
        return ONE_TO_MANY.equals(this);
    }
}
