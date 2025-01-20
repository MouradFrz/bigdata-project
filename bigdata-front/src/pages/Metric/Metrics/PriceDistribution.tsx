import ReactApexChart from 'react-apexcharts';
import { MetricSchema } from '../../../types';
import { useSelector } from 'react-redux';
import Loader from '../../../components/Loader';
import { IRootState } from '../../../store';
import { useGetPriceDistributionQuery } from '../../../store/services/stats';
import { useMemo } from 'react';
import { ApexOptions } from 'apexcharts';

const PriceDistribution = ({ data: metricData }: { data: MetricSchema }) => {
    const isDark = useSelector((state: IRootState) => state.themeConfig.theme === 'dark' || state.themeConfig.isDarkMode);
    const { data, isError, isLoading } = useGetPriceDistributionQuery();

    const chartData = useMemo(() => {
        if (!data) return null;
        return {
            series: [
                {
                    name: 'Average Price',
                    data: data.map((item) => item.avgPrice),
                },
                {
                    name: 'Min Price',
                    data: data.map((item) => item.minPrice),
                },
                {
                    name: 'Max Price',
                    data: data.map((item) => item.maxPrice),
                },
            ],
            options: {
                chart: {
                    type: 'bar' as const,
                    height: 800,
                    stacked: false,
                    toolbar: {
                        show: false,
                    },
                },
                plotOptions: {
                    bar: {
                        horizontal: true,
                        dataLabels: {
                            position: 'top',
                        },
                    },
                },
                colors: ['#4361ee', '#805dca', '#00ab55'],
                dataLabels: {
                    enabled: true,
                    offsetX: 30,
                    style: {
                        fontSize: '12px',
                        colors: ['#fff'],
                    },
                    formatter: function (val: number) {
                        return '$' + val.toFixed(2);
                    },
                },
                stroke: {
                    show: true,
                    width: 1,
                    colors: ['#fff'],
                },
                xaxis: {
                    categories: data.map((item) => `${item.category} (${item.productCount} products)`),
                    labels: {
                        formatter: function (value: string) {
                            // Si la valeur est un nombre, on le formate
                            const numValue = parseFloat(value);
                            if (!isNaN(numValue)) {
                                return '$' + numValue.toFixed(2);
                            }
                            // Sinon on retourne la valeur telle quelle
                            return value;
                        },
                    },
                },
                yaxis: {
                    title: {
                        text: 'Categories',
                    },
                },
                tooltip: {
                    theme: isDark ? 'dark' : 'light',
                    shared: true,
                    intersect: false,
                    y: {
                        formatter: function (val: number) {
                            return '$' + val.toFixed(2);
                        },
                    },
                },
                legend: {
                    position: 'top',
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

export default PriceDistribution;
