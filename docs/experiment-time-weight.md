# Experiment Time Weight

Use `time_weight` to represent expected execution time for CSV/text experiment types.

## Standard Benchmark Dataset

- 50,000 rows
- 20 columns
- CSV file
- No huge text blobs
- Normal laptop/server worker

## Scale

| time_weight | Expected runtime |
| --- | --- |
| 1 | Usually completes in under 10 seconds |
| 2 | Around 10-30 seconds |
| 3 | Around 30-60 seconds |
| 5 | Around 1-3 minutes |
| 8 | Around 3-7 minutes |
| 13 | Around 7-15 minutes |
| 20 | More than 15 minutes |
