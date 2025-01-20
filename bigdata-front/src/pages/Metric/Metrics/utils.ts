export interface ReviewData {
    timestamp: number;
    reviewCount: number;
    averageRating: number;
}

export interface GroupedReviewData {
    month: string;
    reviewCount: number;
    averageRating: number;
}

export function groupByMonthIgnoringYear(data: ReviewData[]): GroupedReviewData[] {
    // Helper function to get the month name (ignores year)
    const getMonthName = (timestamp: number): string => {
        const date = new Date(timestamp);
        const options: Intl.DateTimeFormatOptions = { month: 'long' };
        return date.toLocaleDateString('en-US', options);
    };

    // Group data by month (ignoring the year)
    const groupedData: { [key: string]: { reviewCount: number; averageRating: number; count: number } } = {};

    data.forEach((entry) => {
        const monthName = getMonthName(entry.timestamp);

        if (!groupedData[monthName]) {
            groupedData[monthName] = { reviewCount: 0, averageRating: 0, count: 0 };
        }

        groupedData[monthName].reviewCount += entry.reviewCount;
        groupedData[monthName].averageRating += entry.averageRating;
        groupedData[monthName].count += 1;
    });

    // Prepare the final result, calculating the average rating for each month
    const result = Object.keys(groupedData).map((month) => {
        const { reviewCount, averageRating, count } = groupedData[month];
        return {
            month,
            reviewCount,
            averageRating: averageRating / count,
        };
    });

    // Order months by their natural order (January, February, ..., December)
    const monthOrder = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

    // Sort the result by month order
    return result.sort((a, b) => monthOrder.indexOf(a.month) - monthOrder.indexOf(b.month));
}
