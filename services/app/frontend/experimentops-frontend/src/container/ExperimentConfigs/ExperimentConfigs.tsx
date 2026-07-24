import { useTranslation } from 'react-i18next'

import { DataTable } from '../../components/DataTable'
import { JsonEditorDialog } from '../../components/JsonViewer'
import { TableSkeleton } from '../../components/TableSkeleton'
import { useDocumentTitle } from '../../hooks'
import {
  PageSkeleton,
  PageState,
  ProjectFrame,
  SectionState,
} from '../ProjectDetails/projectDetails.shared'
import { getErrorMessage } from '../ProjectDetails/projectDetails.utils'
import { CreateExperimentConfigDialog } from './CreateExperimentConfigDialog'
import { getExperimentConfigColumns } from './columns'
import { useExperimentConfigsContainer } from './useExperimentConfigsContainer'

export function ExperimentConfigs() {
  const { t } = useTranslation()
  const container = useExperimentConfigsContainer()

  useDocumentTitle(t('configs.title'))

  if (container.projectQuery.isLoading) return <PageSkeleton />
  if (container.projectQuery.error) {
    return (
      <PageState
        message={getErrorMessage(
          container.projectQuery.error,
          t('datasets.errors.loadProject'),
        )}
        title={t('project.errors.loadTitle')}
        tone="error"
      />
    )
  }
  if (!container.projectQuery.data) {
    return (
      <PageState
        message={t('project.notAvailable')}
        title={t('project.errors.notFoundTitle')}
      />
    )
  }

  return (
    <ProjectFrame activeTab="experiments" project={container.projectQuery.data}>
      <div className="mb-2">
        <h2 className="font-heading text-xl font-semibold text-secondary">
          {t('configs.title')}
        </h2>
      </div>
      <div>
        {!container.canListConfigs ? (
          <SectionState message={t('configs.permissionDenied')} />
        ) : container.configsQuery.isLoading ? (
          <TableSkeleton />
        ) : container.configsQuery.error ? (
          <SectionState
            message={getErrorMessage(
              container.configsQuery.error,
              t('configs.errors.load'),
            )}
            tone="error"
          />
        ) : (
          <DataTable
            columns={getExperimentConfigColumns({
              onEdit: container.setEditingConfig,
              t,
            })}
            data={container.configsQuery.data?.data ?? []}
            emptyMessage={t('configs.empty')}
            getRowKey={(config) => config.uuid}
            pagination={{
              onPageChange: container.setPage,
              page: container.page,
              totalItems: container.configsQuery.data?.totalElements ?? 0,
              totalPages: container.totalPages,
            }}
            searchPlaceholder={t('configs.search')}
            toolbarEnd={
              <>
                {container.canAddConfig ? (
                  <button
                    className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90"
                    onClick={() => container.setIsCreateOpen(true)}
                    type="button"
                  >
                    + {t('configs.add')}
                  </button>
                ) : null}
              </>
            }
          />
        )}
      </div>

      {container.isCreateOpen && (
        <CreateExperimentConfigDialog
          experimentTypes={container.experimentTypesQuery.data?.data ?? []}
          isLoadingExperimentTypes={container.experimentTypesQuery.isLoading}
          isPending={container.createConfigMutation.isPending}
          onClose={() => container.setIsCreateOpen(false)}
          onSubmit={container.handleCreateConfig}
        />
      )}

      {container.editingConfig && (
        <JsonEditorDialog
          getValidationErrors={container.getEditingConfigValidationErrors}
          isSubmitting={container.updateConfigMutation.isPending}
          isValidationLoading={container.experimentTypesQuery.isLoading}
          onClose={() => container.setEditingConfig(null)}
          onSubmit={container.handleUpdateConfig}
          title={container.editingConfig.name}
          value={container.editingConfig.config}
        />
      )}
    </ProjectFrame>
  )
}
