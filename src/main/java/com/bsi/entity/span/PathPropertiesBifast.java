package com.bsi.entity.span;

import java.io.File;

public class PathPropertiesBifast {
    private String pathProp;
    private String propName;

    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/home/span/apps/bo2span/data/config/";
        }
        String trimmed = path.trim();
        if (!trimmed.endsWith("/") && !trimmed.endsWith("\\")) {
            return trimmed + File.separator;
        }
        return trimmed;
    }


    public PathPropertiesBifast(String pathProp, String propName){
        if (pathProp != null && !pathProp.trim().isEmpty()) {
            this.pathProp = normalizePath(pathProp);
        } else {
            this.pathProp = normalizePath(this.pathProp);
        }
        if (propName != null && !propName.trim().isEmpty()) {
            this.propName = propName.trim();
        } else {
            this.propName = "bo2span";
        }
    }

    public String getPathProp(){
        return this.pathProp;
    }

    public String getPropName(){
        return this.propName;
    }
}
