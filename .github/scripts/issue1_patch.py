from pathlib import Path

path = Path("app/src/main/java/com/benclawbot/cmfflow/ProductApp.kt")
text = path.read_text()
old = '''@Composable
private fun HealthResultRow(result: ProbeResult) {
    val ok = result.error == null && result.recordCount > 0
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(if (ok) Icons.Filled.CheckCircle else Icons.Filled.Favorite, null, tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(result.type.replace('_', ' ').replaceFirstChar(Char::uppercase), fontWeight = FontWeight.SemiBold)
            Text(
                if (result.error != null) "Unavailable" else "${result.recordCount} records · ${formatEpochShort(result.latestEpochMs)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
'''
new = '''@Composable
private fun HealthResultRow(result: ProbeResult) {
    val ok = result.error == null && result.recordCount > 0
    val summary = when {
        result.error != null -> "Unavailable · ${result.error}"
        result.recordCount == 0 -> "No records in the last 7 days"
        else -> "${result.recordCount} records · ${result.dataPointCount} points · latest ${formatEpochShort(result.latestEpochMs)}"
    }
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Icon(if (ok) Icons.Filled.CheckCircle else Icons.Filled.Favorite, null, tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(result.type.replace('_', ' ').replaceFirstChar(Char::uppercase), fontWeight = FontWeight.SemiBold)
            Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            if (result.origins.isNotEmpty()) {
                Text("Source · ${result.origins.sorted().joinToString()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            healthValidationHint(result)?.let { hint ->
                Text(hint, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun healthValidationHint(result: ProbeResult): String? = when (result.type) {
    "heart_rate" -> "Validation: measure heart rate now, sync Nothing X, rerun diagnostics, and confirm the latest time advances to the fresh measurement."
    "exercise" -> "Validation: record a short workout, sync Nothing X, rerun diagnostics, and confirm an exercise record appears with the new time."
    else -> null
}
'''
if old not in text:
    raise SystemExit("HealthResultRow anchor not found")
path.write_text(text.replace(old, new, 1))
