package com.kamesuta.physxmc.command;

enum CommandNames {
    MAIN("physxmc"),
    RESET("reset"),
    DEBUG("debug"),
    DENSITY("density"),
    UPDATECHUNKS("updatechunk"),
    SUMMONTESTOBJECT("summontestobject"),
    GRAVITY("gravity"),
    ;
    
    private final String name;

    CommandNames(String name) {
        this.name = name;
    }
    
    public String get(){
        return name;
    }
}
