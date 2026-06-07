package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.EntityNotFoundException;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.platformapi.dal.repository.WorkspaceRepository;
import com.experimentops.platformapi.model.entity.Workspace;
import com.experimentops.platformapi.model.type.StatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkspaceQueryService {
    private final WorkspaceRepository workspaceRepository;

    public Workspace getActiveWorkspaceNameByUuid(String workspaceUuid) {
        return workspaceRepository
                .findByUuidAndStatusAndEnabled(workspaceUuid, StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new EntityNotFoundException("Workspace uuid", workspaceUuid));
    }

    public Workspace getActiveWorkspaceByName(String workspaceName){
        return workspaceRepository
                .findByNameAndStatusAndEnabled(workspaceName, StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new ValidationException(
                        ErrorCode.WORKSPACE_NOT_FOUND,
                        ErrorCode.WORKSPACE_NOT_FOUND.getMessage()
                ));
    }
}
