{{- define "experimentops.labels" -}}
app.kubernetes.io/part-of: experimentops
{{- end -}}
{{- define "experimentops.appLabels" -}}
app.kubernetes.io/name: {{ . }}
app.kubernetes.io/part-of: experimentops
{{- end -}}
