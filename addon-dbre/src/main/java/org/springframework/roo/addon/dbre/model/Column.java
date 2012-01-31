package org.springframework.roo.addon.dbre.model;

import static org.springframework.roo.model.JavaType.OBJECT;
import static org.springframework.roo.model.JdkJavaType.ARRAY;
import static org.springframework.roo.model.JdkJavaType.BIG_DECIMAL;
import static org.springframework.roo.model.JdkJavaType.BLOB;
import static org.springframework.roo.model.JdkJavaType.CLOB;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JdkJavaType.REF;
import static org.springframework.roo.model.JdkJavaType.STRUCT;

import java.sql.Types;

import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Represents a column in the database model.
 * 
 * @author Alan Stewart.
 * @since 1.1
 */
public class Column {
    private boolean autoIncrement;
    private final int columnSize;
    private final int dataType;
    private String defaultValue;
    private String description;
    private JavaType javaType;
    private String jdbcType;
    private final String name;
    private boolean primaryKey;
    private boolean required;
    private int scale = 0;
    private final String typeName;
    private boolean unique;

    Column(final String name, final int dataType, final String typeName,
            final int columnSize, final int scale) {
        Assert.hasText(name, "Column name required");
        this.name = name;
        this.dataType = dataType;
        this.typeName = typeName;
        this.columnSize = columnSize;
        this.scale = scale;
        init();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Column other = (Column) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    public int getColumnSize() {
        return columnSize;
    }

    public int getDataType() {
        return dataType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public String getEscapedName() {
        return name.replaceAll("\\\\", "\\\\\\\\");
    }

    public JavaType getJavaType() {
        return javaType;
    }

    public String getJdbcType() {
        return jdbcType;
    }

    public String getName() {
        return name;
    }

    public int getScale() {
        return scale;
    }

    public String getTypeName() {
        return typeName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    private void init() {
        switch (dataType) {
        case Types.CHAR:
            if (columnSize > 1) {
                jdbcType = "VARCHAR";
                javaType = JavaType.STRING;
            }
            else {
                jdbcType = "CHAR";
                javaType = JavaType.CHAR_OBJECT;
            }
            break;
        case Types.VARCHAR:
            jdbcType = "VARCHAR";
            javaType = JavaType.STRING;
            break;
        case Types.LONGVARCHAR:
            jdbcType = "LONGVARCHAR";
            javaType = JavaType.STRING;
            break;
        case Types.NUMERIC:
            jdbcType = "NUMERIC";
            javaType = BIG_DECIMAL;
            break;
        case Types.DECIMAL:
            jdbcType = "DECIMAL";
            javaType = BIG_DECIMAL;
            break;
        case Types.BOOLEAN:
            jdbcType = "BOOLEAN";
            javaType = JavaType.BOOLEAN_OBJECT;
            break;
        case Types.BIT:
            jdbcType = "BIT";
            javaType = JavaType.BOOLEAN_OBJECT;
            break;
        case Types.TINYINT:
            jdbcType = "TINYINT";
            javaType = columnSize > 1 ? JavaType.SHORT_OBJECT
                    : JavaType.BOOLEAN_OBJECT; // ROO-1860
            break;
        case Types.SMALLINT:
            jdbcType = "SMALLINT";
            javaType = JavaType.SHORT_OBJECT;
            break;
        case Types.INTEGER:
            jdbcType = "INTEGER";
            javaType = JavaType.INT_OBJECT;
            break;
        case Types.BIGINT:
            jdbcType = "BIGINT";
            javaType = JavaType.LONG_OBJECT;
            break;
        case Types.REAL:
            jdbcType = "REAL";
            javaType = JavaType.FLOAT_OBJECT;
            break;
        case Types.FLOAT:
            jdbcType = "FLOAT";
            javaType = JavaType.DOUBLE_OBJECT;
            break;
        case Types.DOUBLE:
            jdbcType = "DOUBLE";
            javaType = JavaType.DOUBLE_OBJECT;
            break;
        case Types.BINARY:
            jdbcType = "BINARY";
            javaType = JavaType.BYTE_ARRAY_PRIMITIVE;
            break;
        case Types.VARBINARY:
            jdbcType = "VARBINARY";
            javaType = JavaType.BYTE_ARRAY_PRIMITIVE;
            break;
        case Types.LONGVARBINARY:
            jdbcType = "LONGVARBINARY";
            javaType = JavaType.BYTE_ARRAY_PRIMITIVE;
            break;
        case Types.DATE:
            jdbcType = "DATE";
            javaType = DATE;
            break;
        case Types.TIME:
            jdbcType = "TIME";
            javaType = DATE;
            break;
        case Types.TIMESTAMP:
            jdbcType = "TIMESTAMP";
            javaType = DATE;
            break;
        case Types.CLOB:
            jdbcType = "CLOB";
            javaType = CLOB;
            break;
        case Types.BLOB:
            jdbcType = "BLOB";
            javaType = BLOB;
            break;
        case Types.ARRAY:
            jdbcType = "ARRAY";
            javaType = ARRAY;
            break;
        case Types.DISTINCT:
            jdbcType = "DISTINCT";
            javaType = JavaType.STRING;
            break;
        case Types.REF:
            jdbcType = "REF";
            javaType = REF;
            break;
        case Types.STRUCT:
            jdbcType = "STRUCT";
            javaType = STRUCT;
            break;
        case Types.NULL:
            jdbcType = "NULL";
            break;
        case Types.JAVA_OBJECT:
            jdbcType = "JAVA_OBJECT";
            javaType = OBJECT;
            break;
        case Types.OTHER:
            jdbcType = "OTHER";
            javaType = JavaType.STRING;
            break;
        default:
            jdbcType = "VARCHAR";
            javaType = JavaType.STRING;
            break;
        }
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setAutoIncrement(final boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
    }

    public void setDefaultValue(final String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setPrimaryKey(final boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public void setRequired(final boolean required) {
        this.required = required;
    }

    public void setUnique(final boolean unique) {
        this.unique = unique;
    }

    @Override
    public String toString() {
        return String
                .format("Column [name=%s, dataType=%s, typeName=%s, columnSize=%s, scale=%s, description=%s, primaryKey=%s, required=%s, unique=%s, autoIncrement=%s, jdbcType=%s, javaType=%s, defaultValue=%s]",
                        name, dataType, typeName, columnSize, scale,
                        description, primaryKey, required, unique,
                        autoIncrement, jdbcType, javaType, defaultValue);
    }
}
