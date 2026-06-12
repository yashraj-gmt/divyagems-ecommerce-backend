package com.divyagems.ecommerce.entity;

import com.divyagems.ecommerce.enums.TagTypeEnum;
import jakarta.persistence.*;
import lombok.*;

/**
 * Tag entity for spiritual/astrology product properties.
 * Tags are categorized by TagTypeEnum (Planet, Chakra, Element, etc.)
 * and are linked to products via a many-to-many join table.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tags", indexes = {
        @Index(name = "idx_tag_slug", columnList = "slug"),
        @Index(name = "idx_tag_type", columnList = "tag_type")
})
public class Tag extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 150)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_type", nullable = false, length = 30)
    private TagTypeEnum tagType;
}
