import React from 'react';

const CardSkeleton = () => (
  <div className="bg-white rounded-2xl border border-slate-100 p-6 shadow-sm animate-pulse">
    <div className="flex items-center gap-4 mb-4">
      <div className="w-12 h-12 bg-slate-100 rounded-xl"></div>
      <div className="flex-1">
        <div className="h-2.5 bg-slate-100 rounded-full w-20 mb-2"></div>
        <div className="h-5 bg-slate-100 rounded-full w-24"></div>
      </div>
    </div>
    <div className="space-y-2">
      <div className="h-2 bg-slate-100 rounded-full w-full"></div>
      <div className="h-2 bg-slate-100 rounded-full w-3/4"></div>
    </div>
  </div>
);

const ListSkeleton = () => (
  <div className="bg-white rounded-2xl border border-slate-100 overflow-hidden shadow-sm animate-pulse">
    <div className="px-6 py-4 border-b border-slate-50 flex justify-between items-center">
      <div className="h-4 bg-slate-100 rounded-full w-32"></div>
      <div className="h-8 bg-slate-100 rounded-xl w-24"></div>
    </div>
    <div className="divide-y divide-slate-50">
      {[1, 2, 3, 4, 5].map((i) => (
        <div key={i} className="px-6 py-5 flex items-center gap-6">
          <div className="flex-1">
            <div className="h-4 bg-slate-100 rounded-full w-1/3 mb-2"></div>
            <div className="h-3 bg-slate-100 rounded-full w-1/4"></div>
          </div>
          <div className="h-6 bg-slate-100 rounded-full w-20"></div>
          <div className="h-6 bg-slate-100 rounded-full w-24"></div>
          <div className="flex gap-2">
            <div className="w-8 h-8 bg-slate-100 rounded-lg"></div>
            <div className="w-8 h-8 bg-slate-100 rounded-lg"></div>
          </div>
        </div>
      ))}
    </div>
  </div>
);

const DetailSkeleton = () => (
  <div className="space-y-6 animate-pulse">
    <div className="flex justify-between items-center px-2">
      <div className="h-8 bg-slate-200 rounded-xl w-64"></div>
      <div className="h-10 bg-slate-200 rounded-xl w-32"></div>
    </div>
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div className="lg:col-span-2 space-y-6">
        <div className="card h-64"></div>
        <div className="card h-48"></div>
      </div>
      <div className="space-y-6">
        <div className="card h-40"></div>
        <div className="card h-80"></div>
      </div>
    </div>
  </div>
);

const FormSkeleton = () => (
  <div className="max-w-2xl mx-auto space-y-6 animate-pulse">
    <div className="h-8 bg-slate-200 rounded-xl w-48 mb-8"></div>
    <div className="card p-8 space-y-8">
      {[1, 2, 3, 4, 5].map(i => (
        <div key={i} className="space-y-3">
          <div className="h-3 bg-slate-100 rounded-full w-24"></div>
          <div className="h-12 bg-slate-50 border border-slate-100 rounded-xl w-full"></div>
        </div>
      ))}
      <div className="flex gap-4 pt-4">
        <div className="h-12 bg-slate-200 rounded-xl flex-1"></div>
        <div className="h-12 bg-slate-100 rounded-xl flex-1"></div>
      </div>
    </div>
  </div>
);

const LoadingSkeleton = ({ type = 'list' }) => {
  switch (type) {
    case 'grid':
      return (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 p-6">
          {[1, 2, 3, 4, 5, 6, 7, 8].map(i => <CardSkeleton key={i} />)}
        </div>
      );
    case 'detail':
      return <DetailSkeleton />;
    case 'form':
      return <FormSkeleton />;
    case 'list':
    default:
      return <ListSkeleton />;
  }
};

export default LoadingSkeleton;