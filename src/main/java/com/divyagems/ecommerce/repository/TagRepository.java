package com.divyagems.ecommerce.repository;

import com.divyagems.ecommerce.entity.Tag;
import com.divyagems.ecommerce.enums.TagTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findByTagType(TagTypeEnum tagType);

    List<Tag> findByTagTypeAndNameIn(TagTypeEnum tagType, List<String> names);

    List<Tag> findByIdIn(List<UUID> ids);

    Optional<Tag> findByName(String name);

    Optional<Tag> findByTagTypeAndName(TagTypeEnum tagType, String name);
}
