package org.example.lastcall.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "페이징 처리된 응답 DTO")
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {
    @Schema(description = "페이지 내 데이터 리스트")
    private final List<T> content;

    @Schema(description = "전체 데이터 개수", example = "128")
    private final long totalElements;

    @Schema(description = "전체 페이지 수", example = "13")
    private final int totalPages;

    @Schema(description = "페이지당 데이터 개수", example = "10")
    private final int size;

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private final int number;

    public PageResponse(List<T> content,
                        long totalElements,
                        int totalPages, int size, int number
    ) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.size = size;
        this.number = number;
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber()
        );
    }

    public static <R> PageResponse<R> of(Page<?> page, List<R> mappedContent) {
        return new PageResponse<>(
                mappedContent,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber()
        );
    }
}
