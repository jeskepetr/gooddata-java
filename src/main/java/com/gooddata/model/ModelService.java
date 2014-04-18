/*
 * Copyright (C) 2007-2014, GoodData(R) Corporation. All rights reserved.
 */
package com.gooddata.model;

import com.gooddata.AbstractService;
import com.gooddata.GoodDataRestException;
import com.gooddata.project.Project;
import org.apache.commons.io.IOUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * TODO
 */
public class ModelService extends AbstractService {

    public ModelService(RestTemplate restTemplate) {
        super(restTemplate);
    }

    public ModelDiff getProjectModelDiff(Project project, DiffRequest diffRequest) {
        try {
            final DiffTask diffTask = restTemplate.postForObject(DiffRequest.URI, diffRequest, DiffTask.class, project.getId());
            return poll(diffTask.getUri(), new StatusOkConditionCallback(), ModelDiff.class);
        } catch (GoodDataRestException | RestClientException e) {
            throw new ModelException("Unable to get project model diff", e);
        }
    }

    public ModelDiff getProjectModelDiff(Project project, String targetModel) {
        return getProjectModelDiff(project, new DiffRequest(targetModel));
    }

    public ModelDiff getProjectModelDiff(Project project, InputStream targetModel) {
        try {
            return getProjectModelDiff(project, new String(IOUtils.toByteArray(targetModel)));
        } catch (IOException e) {
            throw new ModelException("Can't read target model", e);
        }
    }

    public void updateProjectModel(Project project, ModelDiff projectModelDiff) {
        for (ModelDiff.UpdateScript updateScript : projectModelDiff.getUpdateScripts()) {
            for (String maql : updateScript.getMaqlChunks()) {
                updateProjectModel(project, maql);
            }
        }
    }

    public void updateProjectModel(Project project, String maqlDdl) {
        try {
            final MaqlDdlLinks linkEntries = restTemplate.postForObject(MaqlDdl.URI, new MaqlDdl(maqlDdl), MaqlDdlLinks.class, project.getId());
            final MaqlDdlTaskStatus maqlDdlTaskStatus = poll(linkEntries.getStatusLink(), MaqlDdlTaskStatus.class);
            if (!maqlDdlTaskStatus.isSuccess()) {
                 throw new ModelException("Update project model finished with status " + maqlDdlTaskStatus.getStatus());
            }
        } catch (GoodDataRestException | RestClientException e) {
            throw new ModelException("Unable to update project model", e);
        }
    }



}
