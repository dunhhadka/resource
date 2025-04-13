package org.example.product.product.domain.product.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(name = "ProductLogs")
@NoArgsConstructor
public class ProductLog {

    public static final String VERB_ADD = "add";
    public static final String VERB_UPDATE = "update";
    public static final String VERB_DELETE = "delete";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int storeId;

    private int productId;

    @Size(max = 50)
    @NotBlank
    private String verb;

    @Lob
    private String data;

    @NotNull
    private Instant createdOn;
}
