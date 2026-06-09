{{/*
Expand the name of the chart.
*/}}
{{- define "beer-catalogue.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
Truncated to 63 chars (DNS naming spec limit).
If the release name already contains the chart name, use the release name as-is.
*/}}
{{- define "beer-catalogue.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Chart label value: name-version (+ replaced by _ to satisfy label constraints).
*/}}
{{- define "beer-catalogue.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels applied to every resource managed by this chart.
*/}}
{{- define "beer-catalogue.labels" -}}
helm.sh/chart: {{ include "beer-catalogue.chart" . }}
{{ include "beer-catalogue.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels used in Deployment matchLabels and Service selector.
Stable across upgrades — do NOT add mutable fields here.
*/}}
{{- define "beer-catalogue.selectorLabels" -}}
app.kubernetes.io/name: {{ include "beer-catalogue.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
