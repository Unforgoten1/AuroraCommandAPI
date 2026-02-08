package dev.aurora.pagination;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic paginated list wrapper.
 * Divides a list into pages for easier display.
 *
 * @param <T> The type of elements in the list
 */
public class PaginatedList<T> {
    private final List<T> allItems;
    private final int itemsPerPage;
    private final int totalPages;

    /**
     * Create a paginated list.
     *
     * @param items List of all items
     * @param itemsPerPage Number of items per page
     */
    public PaginatedList(List<T> items, int itemsPerPage) {
        this.allItems = new ArrayList<>(items);
        this.itemsPerPage = Math.max(1, itemsPerPage);
        this.totalPages = (int) Math.ceil((double) allItems.size() / this.itemsPerPage);
    }

    /**
     * Get items for a specific page.
     *
     * @param page Page number (1-indexed)
     * @return List of items on that page
     */
    public List<T> getPage(int page) {
        int safePage = Math.max(1, Math.min(page, totalPages));
        int startIndex = (safePage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allItems.size());

        if (startIndex >= allItems.size()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(allItems.subList(startIndex, endIndex));
    }

    /**
     * Check if a page number is valid.
     *
     * @param page Page number (1-indexed)
     * @return True if valid page
     */
    public boolean isValidPage(int page) {
        return page >= 1 && page <= totalPages;
    }

    /**
     * Check if there is a next page.
     *
     * @param currentPage Current page number (1-indexed)
     * @return True if next page exists
     */
    public boolean hasNextPage(int currentPage) {
        return currentPage < totalPages;
    }

    /**
     * Check if there is a previous page.
     *
     * @param currentPage Current page number (1-indexed)
     * @return True if previous page exists
     */
    public boolean hasPreviousPage(int currentPage) {
        return currentPage > 1;
    }

    /**
     * Get the total number of pages.
     *
     * @return Total pages
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * Get the total number of items.
     *
     * @return Total items
     */
    public int getTotalItems() {
        return allItems.size();
    }

    /**
     * Get the number of items per page.
     *
     * @return Items per page
     */
    public int getItemsPerPage() {
        return itemsPerPage;
    }

    /**
     * Get all items.
     *
     * @return All items
     */
    public List<T> getAllItems() {
        return new ArrayList<>(allItems);
    }
}
