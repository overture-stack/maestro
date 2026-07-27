# Exclusion Rules

Maestro supports data publication controls by providing configurable exclusion rules. These rules allow you to omit specific analyses from being indexed based on metadata tags assigned by Song. You can use Study, Analysis, File, Sample, Specimen, and Donor IDs to exclude data from indexing.

## Setting Up Exclusion Rules

To configure exclusion rules, update your `.env.maestro` file with the following properties. Each property is a comma-separated list of the IDs you want to exclude from indexing.

```bash
# ---------------------------
# EXCLUSION RULES CONFIGS
# ---------------------------
# Exclusion rules configurations for Maestro

# Exclude by study ID
EXCLUSIONRULES_BYID_STUDYID=TEST-STUDY

# Exclude specific analyses
# EXCLUSIONRULES_BYID_ANALYSIS=531had59-235f-315j-3918-gjaea93ga90j

# Exclude specific files
# EXCLUSIONRULES_BYID_FILE=41ba4fb3-9428-50b5-af6c-d779cd59b04d

# Exclude specific samples
# EXCLUSIONRULES_BYID_SAMPLE=a6381313-gaj3-eaif-95jd-nahnba9gn112

# Exclude specific specimens
# EXCLUSIONRULES_BYID_SPECIMEN=j928shgh-bme9-gka7-vac8-ga239sdaig98

# Exclude specific donors
# EXCLUSIONRULES_BYID_DONOR=DO232991
```

:::info
For any configurations to take effect, make sure to uncomment the exclusion rule you are adding, save your changes, and restart the Maestro service.
:::

## Usage Guidelines

1. **Multiple Exclusions**: To exclude multiple IDs of the same type, separate them with commas. For example:
   ```bash
   EXCLUSIONRULES_BYID_STUDYID=TEST-STUDY-1,TEST-STUDY-2,TEST-STUDY-3
   ```

2. **Combining Rules**: You can use multiple exclusion rules simultaneously. For instance, you can exclude specific studies and specific files within other studies.

3. **Case Sensitivity**: Ensure that the IDs you enter match exactly with those in your Song metadata, as the exclusion rules are case-sensitive.

4. **Regular Updates**: Regularly review and update your exclusion rules to ensure they align with your current data publication policies.

## Best Practices

- **Documentation**: Keep a separate document explaining the rationale behind each exclusion rule for future reference.
- **Testing**: After implementing new exclusion rules, perform a test indexing to ensure the rules are working as expected.

By following these guidelines, you can effectively control which data gets indexed in Maestro, ensuring compliance with your data publication policies while maintaining the integrity of your searchable dataset.