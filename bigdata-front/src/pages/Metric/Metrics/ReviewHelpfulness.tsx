import ReactApexChart from 'react-apexcharts';
import { MetricSchema } from '../../../types';
import { useSelector } from 'react-redux';
import Loader from '../../../components/Loader';
import { IRootState } from '../../../store';
import { useGetReviewHelpfulnessQuery } from '../../../store/services/stats';
import { useMemo } from 'react';
import { ApexOptions } from 'apexcharts';

const ReviewHelpfulness = ({ data: metricData }: { data: MetricSchema }) => {
    const isDark = useSelector((state: IRootState) => state.themeConfig.theme === 'dark' || state.themeConfig.isDarkMode);
    const { data, isError, isLoading } = useGetReviewHelpfulnessQuery();

    const chartData = useMemo(() => {
        if (!data) return null;
        return {
            series: [
                {
                    name: 'Average Helpful Votes',
                    type: 'column' as const,
                    data: data.map((item) => ({
                        x: item.rating,
                        y: item.averageHelpfulVotes,
                    })),
                },
                {
                    name: 'Review Count',
                    type: 'line' as const,
                    data: data.map((item) => ({
                        x: item.rating,
                        y: item.reviewCount,
                    })),
                },
            ],
            options: {
                chart: {
                    height: 800,
                    type: 'line' as const,
                    toolbar: {
                        show: false,
                    },
                },
                stroke: {
                    width: [0, 4],
                },
                title: {
                    text: 'Review Helpfulness by Rating',
                },
                dataLabels: {
                    enabled: true,
                    enabledOnSeries: [1],
                },
                labels: data.map((item) => item.rating.toString()),
                colors: ['#4361ee', '#805dca'],
                xaxis: {
                    type: 'category',
                    title: {
                        text: 'Rating',
                    },
                },
                yaxis: [
                    {
                        title: {
                            text: 'Average Helpful Votes',
                        },
                        labels: {
                            formatter: function (value: number) {
                                return value.toFixed(2);
                            },
                        },
                    },
                    {
                        opposite: true,
                        title: {
                            text: 'Number of Reviews',
                        },
                        labels: {
                            formatter: function (value: number) {
                                return value.toFixed(0);
                            },
                        },
                    },
                ],
                tooltip: {
                    theme: isDark ? 'dark' : 'light',
                    shared: true,
                    intersect: false,
                    y: {
                        formatter: function (y: number, { seriesIndex }: { seriesIndex: number }) {
                            if (seriesIndex === 0) {
                                return y.toFixed(2) + ' votes';
                            }
                            return y.toFixed(0) + ' reviews';
                        },
                    },
                },
                legend: {
                    horizontalAlign: 'left',
                },
                grid: {
                    borderColor: isDark ? '#191E3A' : '#E0E6ED',
                },
            } as ApexOptions,
        };
    }, [data, isDark]);

    if (isError) return <>Something went wrong</>;
    if (isLoading)
        return (
            <div className="flex justify-center">
                <Loader />
            </div>
        );

    return (
        <>
            <h1 className="font-extrabold text-3xl">{metricData.title}</h1>
            <p className="my-2">{metricData.description}</p>
            <ReactApexChart series={chartData?.series} options={chartData?.options} className="rounded-lg bg-white dark:bg-black overflow-hidden" height={800} />
        </>
    );
};

export default ReviewHelpfulness;
