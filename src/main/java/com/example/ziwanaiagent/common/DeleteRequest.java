package com.example.ziwanaiagent.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {
    private static final long serialVersionUID = 3584070451502785241L;
    /**
     * id
     */
    private Long id;


}
