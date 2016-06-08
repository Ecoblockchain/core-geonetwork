/*
 * Copyright (C) 2001-2016 Food and Agriculture Organization of the
 * United Nations (FAO-UN), United Nations World Food Programme (WFP)
 * and United Nations Environment Programme (UNEP)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 *
 * Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 * Rome - Italy. email: geonetwork@osgeo.org
 */

package org.fao.geonet.api.categories;

import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.api.API;
import org.fao.geonet.api.exception.ResourceNotFoundException;
import org.fao.geonet.domain.Language;
import org.fao.geonet.domain.MetadataCategory;
import org.fao.geonet.repository.LanguageRepository;
import org.fao.geonet.repository.MetadataCategoryRepository;
import org.fao.geonet.repository.Updater;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RequestMapping(value = {
    "/api/tags",
    "/api/" + API.VERSION_0_1 +
        "/tags"
})
@Api(value = "tags",
    tags = "tags",
    description = "Tags operations")
@Controller("tags")
public class TagsApi {

    @ApiOperation(
        value = "Get tags",
        notes = "",
        nickname = "getTags")
    @RequestMapping(
        produces = MediaType.APPLICATION_JSON_VALUE,
        method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public List<org.fao.geonet.domain.MetadataCategory> getTags(
    ) throws Exception {
        ApplicationContext appContext = ApplicationContextHolder.get();
        MetadataCategoryRepository categoryRepository =
            appContext.getBean(MetadataCategoryRepository.class);
        return categoryRepository.findAll();
    }


    @ApiOperation(
        value = "Create a tag",
        notes = "If labels are not defined, a default label is created " +
            "with the category name for all languages.",
        nickname = "putTag")
    @RequestMapping(
        method = RequestMethod.PUT,
        consumes = {
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    @PreAuthorize("hasRole('UserAdmin')")
    public ResponseEntity<Integer> putTag(
        @ApiParam(
            name = "category"
        )
        @RequestBody
            MetadataCategory category
    ) throws Exception {
        ApplicationContext appContext = ApplicationContextHolder.get();
        MetadataCategoryRepository categoryRepository =
            appContext.getBean(MetadataCategoryRepository.class);

        MetadataCategory existingCategory = categoryRepository.findOne(category.getId());
        if (existingCategory != null) {
            updateCategory(category.getId(), category, categoryRepository);
        } else {
            // Populate languages if not already set
            LanguageRepository langRepository = appContext.getBean(LanguageRepository.class);
            java.util.List<Language> allLanguages = langRepository.findAll();
            Map<String, String> labelTranslations = category.getLabelTranslations();
            for (Language l : allLanguages) {
                String label = labelTranslations.get(l.getId());
                category.getLabelTranslations().put(l.getId(),
                    label == null ? category.getName() : label);
            }
            categoryRepository.save(category);
        }
        return new ResponseEntity(category.getId(), HttpStatus.CREATED);
    }

    @ApiOperation(
        value = "Get a tag",
        notes = "",
        nickname = "getTag")
    @RequestMapping(
        value = "/{tagIdentifier}",
        produces = {
            MediaType.APPLICATION_JSON_VALUE
        },
        method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public org.fao.geonet.domain.MetadataCategory getTag(
        @ApiParam(
            value = "Tag identifier",
            required = true
        )
        @PathVariable
            Integer tagIdentifier
    ) throws Exception {
        ApplicationContext appContext = ApplicationContextHolder.get();
        MetadataCategoryRepository categoryRepository =
            appContext.getBean(MetadataCategoryRepository.class);
        org.fao.geonet.domain.MetadataCategory category = categoryRepository.findOne(tagIdentifier);
        if (category == null) {
            throw new ResourceNotFoundException(String.format("Category not found"));
        }
        return category;
    }

    @ApiOperation(
        value = "Update a tag",
        notes = "",
        nickname = "updateTag")
    @RequestMapping(
        value = "/{tagIdentifier}",
        method = RequestMethod.PUT)
    @ResponseStatus(value = HttpStatus.OK)
    @PreAuthorize("hasRole('UserAdmin')")
    @ResponseBody
    public ResponseEntity updateTag(
        @ApiParam(
            value = "Tag identifier",
            required = true
        )
        @PathVariable
            int tagIdentifier,
        @ApiParam(
            name = "category"
        )
        @RequestBody
            MetadataCategory category
    ) throws Exception {
        ApplicationContext appContext = ApplicationContextHolder.get();
        MetadataCategoryRepository categoryRepository =
            appContext.getBean(MetadataCategoryRepository.class);

        MetadataCategory existingCategory = categoryRepository.findOne(tagIdentifier);
        if (existingCategory != null) {
            updateCategory(tagIdentifier, category, categoryRepository);
        } else {
            throw new ResourceNotFoundException(String.format(
                "Category with id '%d' does not exist.",
                tagIdentifier
            ));
        }
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    private void updateCategory(
        int categoryIdentifier,
        final MetadataCategory category,
        MetadataCategoryRepository categoryRepository) {
        categoryRepository.update(categoryIdentifier, entity -> {
            entity.setName(category.getName());
            Map<String, String> labelTranslations = category.getLabelTranslations();
            if (labelTranslations != null) {
                entity.getLabelTranslations().clear();
                entity.getLabelTranslations().putAll(labelTranslations);
            }
        });
    }


    @ApiOperation(
        value = "Delete a tag",
        notes = "",
        nickname = "deleteTag")
    @RequestMapping(
        value = "/{tagIdentifier}",
        method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    @PreAuthorize("hasRole('UserAdmin')")
    public ResponseEntity deleteTag(
        @ApiParam(
            value = "Tag identifier",
            required = true
        )
        @PathVariable
            Integer tagIdentifier
    ) throws Exception {
        ApplicationContext appContext = ApplicationContextHolder.get();
        MetadataCategoryRepository categoryRepository =
            appContext.getBean(MetadataCategoryRepository.class);

        MetadataCategory category = categoryRepository.findOne(tagIdentifier);
        if (category != null) {
            if (category.getRecords().size() > 0) {
                throw new RuntimeException(String.format(
                    "Tag '%s' is assigned to %d records. Update records first in order to remove that tag.",
                    category.getName(), // TODO: Return in user language
                    category.getRecords().size()
                ));
            } else {
                categoryRepository.delete(tagIdentifier);
            }
        } else {
            throw new ResourceNotFoundException(String.format(
                "Category with id '%d' does not exist.",
                tagIdentifier
            ));
        }
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }
}