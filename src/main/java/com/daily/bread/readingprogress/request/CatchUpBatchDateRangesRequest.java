package com.daily.bread.readingprogress.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CatchUpBatchDateRangesRequest(
		@NotEmpty @Valid List<CatchUpDateRangeRequest> ranges) {
}
