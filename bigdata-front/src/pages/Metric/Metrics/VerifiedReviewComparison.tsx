import ReactApexChart from 'react-apexcharts';
import { MetricSchema } from '../../../types';
import { useSelector } from 'react-redux';
import Loader from '../../../components/Loader';
import { IRootState } from '../../../store';
import { useGetVerifiedComparisonQuery } from '../../../store/services/stats';
import { useMemo } from 'react';
import { ApexOptions } from 'apexcharts';

const VerifiedReviewComparison = ({ data: metricData }: { data: MetricSchema }) => {
    const isDark = useSelector((state: IRootState) => state.themeConfig.theme === 'dark' || state.themeConfig.isDarkMode);
    const { data, isError, isLoading } = useGetVerifiedComparisonQuery();

    const chartData = useMemo(() => {
        if (!data) return null;
        return {
            series: [
                {
                    name: 'Verified Rating',
                    type: 'line' as const,
                    data: data.map((item) => ({
                        x: item.month,
                        y: item.verifiedRating?.toFixed(2) || 0,
                    })),
                },
                {
                    name: 'Non-Verified Rating',
                    type: 'line' as const,
                    data: data.map((item) => ({
                        x: item.month,
                        y: item.nonVerifiedRating?.toFixed(2) || 0,
                    })),
                },
                {
                    name: 'Verified Count',
                    type: 'column' as const,
                    data: data.map((item) => ({
                        x: item.month,
                        y: item.verifiedCount,
                    })),
                },
                {
                    name: 'Non-Verified Count',
                    type: 'column' as const,
                    data: data.map((item) => ({
                        x: item.month,
                        y: item.nonVerifiedCount,
                    })),
                },
            ],
            options: {
                chart: {
                    height: 800,
                    type: 'line' as const,
                    zoom: {
                        enabled: false,
                    },
                    toolbar: {
                        show: false,
                    },
                },
                colors: ['#4361ee', '#805dca', '#00ab55', '#e7515a'],
                stroke: {
                    width: [2, 2, 0, 0],
                    curve: 'smooth',
                },
                plotOptions: {
                    bar: {
                        columnWidth: '50%',
                    },
                },
                fill: {
                    opacity: [1, 1, 0.85, 0.85],
                },
                labels: data.map((item) => item.month),
                markers: {
                    size: 0,
                },
                xaxis: {
                    type: 'datetime',
                    axisBorder: {
                        color: isDark ? '#191e3a' : '#e0e6ed',
                    },
                },
                yaxis: [
                    {
                        title: {
                            text: 'Rating',
                        },
                        min: 1,
                        max: 5,
                    },
                    {
                        opposite: true,
                        title: {
                            text: 'Review Count',
                        },
                    },
                ],
                tooltip: {
                    theme: isDark ? 'dark' : 'light',
                    shared: true,
                    intersect: false,
                    y: {
                        formatter: function (y: number, { seriesIndex }: { seriesIndex: number }) {
                            if (seriesIndex < 2) {
                                return y?.toFixed(2) + ' stars';
                            }
                            return y?.toFixed(0) + ' reviews';
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

export default VerifiedReviewComparison;
