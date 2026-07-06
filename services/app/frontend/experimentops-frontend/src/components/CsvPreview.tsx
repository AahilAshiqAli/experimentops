export type CsvPreviewData = {
  columns: string[]
  format?: string
  rows: Record<string, unknown>[]
  sampleRowCount?: number
}

type CsvPreviewProps = {
  data: CsvPreviewData
  className?: string
}

function getColumnLabel(index: number) {
  let label = ''
  let current = index + 1

  while (current > 0) {
    current -= 1
    label = String.fromCharCode(65 + (current % 26)) + label
    current = Math.floor(current / 26)
  }

  return label
}

function formatCellValue(value: unknown) {
  if (value === null || value === undefined) return ''
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

/** A read-only, dependency-free spreadsheet view for a parsed CSV response. */
export function CsvPreview({ className = '', data }: CsvPreviewProps) {
  const displayedRowCount = data.rows.length
  const reportedRowCount = data.sampleRowCount ?? displayedRowCount

  return (
    <div className={className}>
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2 text-xs text-slate-500">
        <span>
          {displayedRowCount.toLocaleString()} row
          {displayedRowCount === 1 ? '' : 's'} ×{' '}
          {data.columns.length.toLocaleString()} column
          {data.columns.length === 1 ? '' : 's'}
        </span>
        {reportedRowCount > displayedRowCount ? (
          <span>Showing the first {displayedRowCount.toLocaleString()} rows</span>
        ) : null}
      </div>

      <div className="max-h-[65vh] overflow-auto rounded-lg border border-slate-300 bg-white">
        <table className="border-separate border-spacing-0 text-left text-sm text-slate-700">
          <thead className="sticky top-0 z-20">
            <tr>
              <th
                aria-label="Row numbers"
                className="sticky left-0 z-30 min-w-12 border-b border-r border-slate-300 bg-slate-100 px-3 py-1.5 text-center font-medium text-slate-400"
              />
              {data.columns.map((column, index) => (
                <th
                  className="min-w-40 border-b border-r border-slate-300 bg-slate-100 px-4 py-1.5 text-center text-xs font-semibold text-slate-500 last:border-r-0"
                  key={`${column}-${index}`}
                  scope="col"
                >
                  <span className="block">{getColumnLabel(index)}</span>
                  <span className="mt-0.5 block truncate text-slate-700" title={column}>
                    {column}
                  </span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.rows.map((row, rowIndex) => (
              <tr className="hover:bg-primary/[0.03]" key={rowIndex}>
                <th
                  className="sticky left-0 z-10 border-b border-r border-slate-200 bg-slate-50 px-3 py-2 text-center text-xs font-medium text-slate-400"
                  scope="row"
                >
                  {rowIndex + 1}
                </th>
                {data.columns.map((column, columnIndex) => {
                  const value = formatCellValue(row[column])

                  return (
                    <td
                      className="max-w-96 whitespace-nowrap border-b border-r border-slate-200 px-4 py-2 last:border-r-0"
                      key={`${column}-${columnIndex}`}
                      title={value}
                    >
                      <span className="block max-w-96 truncate">{value || '\u00a0'}</span>
                    </td>
                  )
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
