import ReactApexChart from 'react-apexcharts';
import { MetricSchema } from '../../../types';
import { useSelector } from 'react-redux';
import Loader from '../../../components/Loader';
import { IRootState } from '../../../store';
import { useGetReviewTimelineQuery } from '../../../store/services/stats';
import { useMemo } from 'react';
import { groupByMonthIgnoringYear, ReviewData } from './utils';

const ReviewTimeline = ({ data: metricData }: { data: MetricSchema }) => {
    const isDark = useSelector((state: IRootState) => state.themeConfig.theme === 'dark' || state.themeConfig.isDarkMode);
    const { data, isError, isLoading } = useGetReviewTimelineQuery();
    const formattedData = useMemo(() => {
        if (!data) return [];
        return groupByMonthIgnoringYear(data as unknown as ReviewData[]);
    }, [data]);
    console.log(formattedData);
    const simpleColumnStacked: any = useMemo(() => {
        if (!data) return null;
        return {
            series: [
                { name: 'Note moyenne', data: formattedData.map((item) => item.averageRating.toFixed(2)) },
                { name: 'Nombre de reviews', data: formattedData.map((item) => item.reviewCount.toFixed(2)) },
            ],
            options: {
                chart: {
                    type: 'area',
                    height: 300,
                    zoom: {
                        enabled: false,
                    },
                    toolbar: {
                        show: false,
                    },
                },
                colors: ['#805dca'],
                dataLabels: {
                    enabled: false,
                },
                stroke: {
                    width: 2,
                    curve: 'smooth',
                },
                xaxis: {
                    axisBorder: {
                        color: isDark ? '#191e3a' : '#e0e6ed',
                    },
                },
                yaxis: {
                    opposite: false,
                    labels: {
                        offsetX: 0,
                        formatter: (value: number) => value.toFixed(0),
                    },
                },
                labels: formattedData.map((item) => item.month),
                legend: {
                    horizontalAlign: 'left',
                },
                grid: {
                    borderColor: isDark ? '#191E3A' : '#E0E6ED',
                },
                tooltip: {
                    theme: isDark ? 'dark' : 'light',
                },
            },
        };
    }, [data]);
    if (isError) return <>Something went wrong</>;
    if (isLoading)
        return (
            <div className="flex justify-center">
                <Loader />{' '}
            </div>
        );
    return (
        <>
            <h1 className="font-extrabold text-3xl">{metricData.title}</h1>
            <p className="my-2">{metricData.description}</p>
            <ReactApexChart series={simpleColumnStacked.series} options={simpleColumnStacked.options} className="rounded-lg bg-white dark:bg-black overflow-hidden" type="bar" height={800} />;
        </>
    );
};

export default ReviewTimeline;
