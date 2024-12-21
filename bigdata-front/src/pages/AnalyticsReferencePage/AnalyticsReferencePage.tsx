import React from 'react';
import { AVAILABLE_METRICS } from './availableMetrics';
import { useNavigate } from 'react-router-dom';
function AnalyticsReferencePage() {
    const navigate = useNavigate();
    return (
        <div>
            <h1 className="font-extrabold text-3xl mb-4">Metriques disponibles</h1>
            <div className="flex flex-row flex-wrap">
                {AVAILABLE_METRICS.map((metric) => {
                    return (
                        <div
                            onClick={() => {
                                navigate(`/metric/${metric.id}`);
                            }}
                            key={metric.id}
                            className="cursor-pointer w-[33%] bg-white shadow-[4px_6px_10px_-3px_#bfc9d4] rounded border border-white-light dark:border-[#1b2e4b] dark:bg-[#191e3a] dark:shadow-none hover:bg-white-light"
                        >
                            <div className="py-7 px-6">
                                <div className="bg-[#3b3f5c] mb-5 inline-block p-3 text-[#f1f2f3] rounded-full"></div>
                                <h5 className="text-[#3b3f5c] text-xl font-semibold mb-4 dark:text-white-light">{metric.title}</h5>
                                <p className="text-white-dark">{metric.description}</p>
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
}

export default AnalyticsReferencePage;
